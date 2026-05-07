import os

import pandas as pd
import requests
from rest_framework import status, generics
from rest_framework.generics import ListAPIView
from rest_framework.parsers import MultiPartParser, FormParser
from rest_framework.response import Response
from rest_framework.views import APIView

from .problem_schemas import get_problem_schema, PROBLEM_SCHEMAS
from .problem_schemas.problem_catalog import PROBLEM_FAMILIES, CONSTRAINT_LIBRARY, OBJECTIVE_LIBRARY
from .problem_schemas.rooms import ROOMS_FILE_SCHEMA
from .serializers import ScheduleSerializer, ScheduleListSerializer, ProblemDraftSerializer, RoomDataFileSerializer
from .models import Schedule, ProblemDraft, RoomDataFile
from .services.file_reader import read_schedule_file, extract_columns_and_preview
from .services.column_matcher import match_canonical_fields_to_source_columns


class ScheduleUploadView(APIView):
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request, *args, **kwargs):
        serializer = ScheduleSerializer(data=request.data)
        if serializer.is_valid():
            schedule = serializer.save()
            return Response(
                {
                    "id": schedule.id,
                    "name": schedule.name,
                    "file": schedule.file.url,
                    "uploaded_at": schedule.uploaded_at,
                    "updated_at": schedule.updated_at,
                },
                status=status.HTTP_201_CREATED,
            )

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class ProblemMappingSuggestionsView(APIView):
    def get(self, request, problem_id, *args, **kwargs):
        try:
            problem_draft = ProblemDraft.objects.get(pk=problem_id)
        except ProblemDraft.DoesNotExist:
            return Response(
                {"error": "Problem draft not found."},
                status=status.HTTP_404_NOT_FOUND
            )

        if not problem_draft.uploaded_schedule_id:
            return Response(
                {"error": "Problem draft has no uploaded schedule."},
                status=status.HTTP_400_BAD_REQUEST
            )

        schema = get_problem_schema(problem_draft.problem_subtype)
        if not schema:
            return Response(
                {"error": "Schema not found for this problem subtype."},
                status=status.HTTP_404_NOT_FOUND
            )

        try:
            schedule = problem_draft.uploaded_schedule
        except Exception:
            return Response(
                {"error": "Schedule not found."},
                status=status.HTTP_404_NOT_FOUND
            )

        try:
            df = read_schedule_file(schedule.file.path)
        except Exception as exc:
            return Response(
                {"error": f"Erro ao ler ficheiro: {str(exc)}"},
                status=status.HTTP_400_BAD_REQUEST
            )

        file_info = extract_columns_and_preview(df)
        source_columns = file_info["columns"]

        matches = match_canonical_fields_to_source_columns(
            source_columns,
            schema["fields"]
        )

        preview_lookup = {
            item["source_column"]: item["sample_values"]
            for item in file_info["preview"]
        }

        enriched_matches = []
        for match in matches:
            suggested_column = match.get("suggested_source_column") or ""

            enriched_matches.append({
                "target_field": match["target_field"],
                "target_label": match["target_label"],
                "description": next(
                    (field.get("description", "") for field in schema["fields"] if field["key"] == match["target_field"]),
                    ""
                ),
                "required": next(
                    (field.get("required", False) for field in schema["fields"] if field["key"] == match["target_field"]),
                    False
                ),
                "data_type": next(
                    (field.get("data_type", "string") for field in schema["fields"] if field["key"] == match["target_field"]),
                    "string"
                ),
                "aliases": next(
                    (field.get("aliases", []) for field in schema["fields"] if field["key"] == match["target_field"]),
                    []
                ),
                "suggested_source_column": suggested_column,
                "confidence": match.get("confidence", 0.0),
                "match_type": match.get("match_type", "none"),
                "sample_values": preview_lookup.get(suggested_column, []) if suggested_column else [],
                "available_source_columns": source_columns,
            })

        selected_mappings = {
            item["target_field"]: item["suggested_source_column"] or ""
            for item in enriched_matches
        }

        return Response({
            "problem_id": problem_draft.id,
            "problem_family": problem_draft.problem_family,
            "problem_subtype": problem_draft.problem_subtype,
            "schema": schema,
            "source_columns": source_columns,
            "matches": enriched_matches,
            "selected_mappings": selected_mappings,
            "mode": "suggested",
        })


class ScheduleListView(ListAPIView):
    queryset = Schedule.objects.all().order_by("-uploaded_at")
    serializer_class = ScheduleListSerializer


class ProblemDraftListCreateView(generics.ListCreateAPIView):
    queryset = ProblemDraft.objects.all().order_by("-updated_at")
    serializer_class = ProblemDraftSerializer


class ProblemDraftDetailView(generics.RetrieveUpdateDestroyAPIView):
    queryset = ProblemDraft.objects.all()
    serializer_class = ProblemDraftSerializer


class ProblemCatalogView(APIView):
    def get(self, request, *args, **kwargs):
        return Response({
            "problem_families": PROBLEM_FAMILIES,
            "objective_library": OBJECTIVE_LIBRARY,
            "constraint_library": CONSTRAINT_LIBRARY,
        })


class RoomDataFileUploadView(APIView):
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request, *args, **kwargs):
        serializer = RoomDataFileSerializer(data=request.data)

        if serializer.is_valid():
            room_file = serializer.save()
            return Response(
                {
                    "id": room_file.id,
                    "name": room_file.name,
                    "file": room_file.file.url,
                    "uploaded_at": room_file.uploaded_at,
                    "updated_at": room_file.updated_at,
                },
                status=status.HTTP_201_CREATED,
            )

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class ProblemRoomsFilePreviewView(APIView):
    def get(self, request, problem_id, *args, **kwargs):
        try:
            problem_draft = ProblemDraft.objects.get(pk=problem_id)
        except ProblemDraft.DoesNotExist:
            return Response(
                {"error": "Problem draft not found."},
                status=status.HTTP_404_NOT_FOUND
            )

        if not problem_draft.uploaded_rooms_file_id:
            return Response(
                {"error": "Problem draft has no uploaded rooms.py file."},
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            rooms_file = problem_draft.uploaded_rooms_file
        except Exception:
            return Response(
                {"error": "Rooms file not found."},
                status=status.HTTP_404_NOT_FOUND
            )

        try:
            df = read_schedule_file(rooms_file.file.path)
        except Exception as exc:
            return Response(
                {"error": f"Erro ao ler ficheiro de salas: {str(exc)}"},
                status=status.HTTP_400_BAD_REQUEST
            )

        file_info = extract_columns_and_preview(df)

        schedule_columns = []
        if problem_draft.uploaded_schedule_id:
            try:
                schedule_df = read_schedule_file(problem_draft.uploaded_schedule.file.path)
                schedule_info = extract_columns_and_preview(schedule_df)
                schedule_columns = schedule_info["columns"]
            except Exception:
                schedule_columns = []

        return Response({
            "problem_id": problem_draft.id,
            "rooms_file_id": rooms_file.id,
            "rooms_file_name": rooms_file.name,
            "rooms_source_columns": file_info["columns"],
            "rooms_preview": file_info["preview"],
            "schedule_source_columns": schedule_columns,
            "saved_mapping": problem_draft.rooms_mapping_data or {},
        })


class ProblemRoomsMappingSuggestionsView(APIView):
    def get(self, request, problem_id, *args, **kwargs):
        try:
            problem_draft = ProblemDraft.objects.get(pk=problem_id)
        except ProblemDraft.DoesNotExist:
            return Response(
                {"error": "Problem draft not found."},
                status=status.HTTP_404_NOT_FOUND
            )

        if not problem_draft.uploaded_rooms_file_id:
            return Response(
                {"error": "Problem draft has no uploaded rooms file."},
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            rooms_file = problem_draft.uploaded_rooms_file
        except Exception:
            return Response(
                {"error": "Rooms file not found."},
                status=status.HTTP_404_NOT_FOUND
            )

        try:
            df = read_schedule_file(rooms_file.file.path)
        except Exception as exc:
            return Response(
                {"error": f"Erro ao ler ficheiro de salas: {str(exc)}"},
                status=status.HTTP_400_BAD_REQUEST
            )

        file_info = extract_columns_and_preview(df)
        source_columns = file_info["columns"]

        schema = ROOMS_FILE_SCHEMA
        matches = match_canonical_fields_to_source_columns(
            source_columns,
            schema["fields"]
        )

        preview_lookup = {
            item["source_column"]: item["sample_values"]
            for item in file_info["preview"]
        }

        enriched_matches = []
        for match in matches:
            suggested_column = match.get("suggested_source_column") or ""
            enriched_matches.append({
                "target_field": match["target_field"],
                "target_label": match["target_label"],
                "description": next(
                    (field.get("description", "") for field in schema["fields"] if field["key"] == match["target_field"]),
                    ""
                ),
                "required": next(
                    (field.get("required", False) for field in schema["fields"] if field["key"] == match["target_field"]),
                    False
                ),
                "data_type": next(
                    (field.get("data_type", "string") for field in schema["fields"] if field["key"] == match["target_field"]),
                    "string"
                ),
                "aliases": next(
                    (field.get("aliases", []) for field in schema["fields"] if field["key"] == match["target_field"]),
                    []
                ),
                "suggested_source_column": suggested_column,
                "confidence": match.get("confidence", 0.0),
                "match_type": match.get("match_type", "none"),
                "sample_values": preview_lookup.get(suggested_column, []) if suggested_column else [],
                "available_source_columns": source_columns,
            })

        selected_field_mappings = {
            item["target_field"]: item["suggested_source_column"] or ""
            for item in enriched_matches
        }

        schedule_columns = []
        suggested_schedule_room_column = ""
        if problem_draft.uploaded_schedule_id:
            try:
                schedule_df = read_schedule_file(problem_draft.uploaded_schedule.file.path)
                schedule_info = extract_columns_and_preview(schedule_df)
                schedule_columns = schedule_info["columns"]
                saved_mapping = (problem_draft.mapping_data or {}).get("mapping", {})
                suggested_schedule_room_column = saved_mapping.get("sala", "")
            except Exception:
                schedule_columns = []

        return Response({
            "problem_id": problem_draft.id,
            "schema": schema,
            "rooms_source_columns": source_columns,
            "rooms_preview": file_info["preview"],
            "matches": enriched_matches,
            "selected_field_mappings": selected_field_mappings,
            "schedule_source_columns": schedule_columns,
            "suggested_schedule_room_column": suggested_schedule_room_column,
            "saved_rooms_mapping_data": problem_draft.rooms_mapping_data or {},
        })


class ProblemRoomsMappingSaveView(APIView):
    def post(self, request, problem_id, *args, **kwargs):
        try:
            problem_draft = ProblemDraft.objects.get(pk=problem_id)
        except ProblemDraft.DoesNotExist:
            return Response(
                {"error": "Problem draft not found."},
                status=status.HTTP_404_NOT_FOUND
            )

        payload = request.data if isinstance(request.data, dict) else {}

        field_mappings = payload.get("field_mappings", {})
        linking = payload.get("linking", {})
        characteristics = payload.get("characteristics", {})

        if not isinstance(field_mappings, dict):
            return Response(
                {"error": "O campo 'field_mappings' tem de ser um objeto JSON."},
                status=status.HTTP_400_BAD_REQUEST
            )

        if not isinstance(linking, dict):
            return Response(
                {"error": "O campo 'linking' tem de ser um objeto JSON."},
                status=status.HTTP_400_BAD_REQUEST
            )

        if not isinstance(characteristics, dict):
            return Response(
                {"error": "O campo 'characteristics' tem de ser um objeto JSON."},
                status=status.HTTP_400_BAD_REQUEST
            )

        required_field_keys = ["room_name"]
        missing_required = [
            key for key in required_field_keys
            if not field_mappings.get(key)
        ]
        if missing_required:
            return Response(
                {"error": f"Campos obrigatórios em falta: {', '.join(missing_required)}."},
                status=status.HTTP_400_BAD_REQUEST
            )

        schedule_room_column = linking.get("schedule_room_column", "")
        rooms_file_room_column = linking.get("rooms_file_room_column", "")

        if not schedule_room_column or not rooms_file_room_column:
            return Response(
                {"error": "As colunas de ligação são obrigatórias."},
                status=status.HTTP_400_BAD_REQUEST
            )

        char_format = characteristics.get("format", "")
        char_config = characteristics.get("config", {})

        valid_formats = [
            "single_column_list",
            "multiple_flag_columns",
            "range_flag_columns",
        ]
        if char_format not in valid_formats:
            return Response(
                {"error": "Formato de características inválido."},
                status=status.HTTP_400_BAD_REQUEST
            )

        if not isinstance(char_config, dict):
            return Response(
                {"error": "O campo 'characteristics.config' tem de ser um objeto JSON."},
                status=status.HTTP_400_BAD_REQUEST
            )

        normalized_config = dict(char_config)

        if char_format == "single_column_list":
            source_column = str(char_config.get("source_column", "")).strip()
            separator = str(char_config.get("separator", "")).strip()

            if not source_column:
                return Response(
                    {"error": "Seleciona a coluna de características."},
                    status=status.HTTP_400_BAD_REQUEST
                )

            if not separator:
                return Response(
                    {"error": "Indica o separador das características."},
                    status=status.HTTP_400_BAD_REQUEST
                )

            normalized_config["source_column"] = source_column
            normalized_config["separator"] = separator

        elif char_format == "multiple_flag_columns":
            selected_columns = char_config.get("selected_columns", [])
            selected_values = char_config.get("selected_values")
            active_values = char_config.get("active_values")

            effective_values = selected_values if selected_values is not None else active_values

            if not isinstance(selected_columns, list) or len(selected_columns) == 0:
                return Response(
                    {"error": "Seleciona pelo menos uma coluna de características."},
                    status=status.HTTP_400_BAD_REQUEST
                )

            if not isinstance(effective_values, list) or len(effective_values) == 0:
                return Response(
                    {"error": "Indica os valores que significam ativo."},
                    status=status.HTTP_400_BAD_REQUEST
                )

            selected_columns = [
                str(column).strip()
                for column in selected_columns
                if str(column).strip()
            ]
            effective_values = [
                str(value).strip()
                for value in effective_values
                if str(value).strip()
            ]

            if len(selected_columns) == 0:
                return Response(
                    {"error": "Seleciona pelo menos uma coluna de características."},
                    status=status.HTTP_400_BAD_REQUEST
                )

            if len(effective_values) == 0:
                return Response(
                    {"error": "Indica os valores que significam ativo."},
                    status=status.HTTP_400_BAD_REQUEST
                )

            normalized_config["selected_columns"] = list(dict.fromkeys(selected_columns))
            normalized_config["selected_values"] = list(dict.fromkeys(effective_values))

        elif char_format == "range_flag_columns":
            start_column = str(char_config.get("start_column", "")).strip()
            end_column = str(char_config.get("end_column", "")).strip()
            selected_values = char_config.get("selected_values")
            active_values = char_config.get("active_values")

            effective_values = selected_values if selected_values is not None else active_values

            if not start_column or not end_column:
                return Response(
                    {"error": "Indica a coluna inicial e final do intervalo."},
                    status=status.HTTP_400_BAD_REQUEST
                )

            if not isinstance(effective_values, list) or len(effective_values) == 0:
                return Response(
                    {"error": "Indica os valores que significam ativo."},
                    status=status.HTTP_400_BAD_REQUEST
                )

            effective_values = [
                str(value).strip()
                for value in effective_values
                if str(value).strip()
            ]

            if len(effective_values) == 0:
                return Response(
                    {"error": "Indica os valores que significam ativo."},
                    status=status.HTTP_400_BAD_REQUEST
                )

            normalized_config["start_column"] = start_column
            normalized_config["end_column"] = end_column
            normalized_config["selected_values"] = list(dict.fromkeys(effective_values))

        normalized_characteristics = {
            "format": char_format,
            "config": normalized_config,
        }

        problem_draft.rooms_mapping_data = {
            "field_mappings": field_mappings,
            "linking": linking,
            "characteristics": normalized_characteristics,
        }
        problem_draft.save(update_fields=["rooms_mapping_data", "updated_at"])

        return Response({
            "message": "Mapping das salas guardado com sucesso.",
            "rooms_mapping_data": problem_draft.rooms_mapping_data,
        })

class ProblemSendToJavaView(APIView):
    JAVA_BACKEND_URL = "http://localhost:8080/api/problems/run"

    def post(self, request, problem_id, *args, **kwargs):
        try:
            problem_draft = ProblemDraft.objects.get(pk=problem_id)
        except ProblemDraft.DoesNotExist:
            return Response(
                {"error": "Problem draft not found."},
                status=status.HTTP_404_NOT_FOUND
            )

        payload = {
            "problem_id": problem_draft.id,
            "name": problem_draft.name,
            "problem_type": problem_draft.problem_family,
            "problem_subtype": problem_draft.problem_subtype,
            "schedule_file_id": problem_draft.uploaded_schedule_id,
            "rooms_file_id": problem_draft.uploaded_rooms_file_id or {},
            "mapping_data": problem_draft.mapping_data or {},
            "rooms_mapping_data": problem_draft.rooms_mapping_data or {},
            "objectives": problem_draft.selected_objectives or [],
            "constraints": problem_draft.selected_constraints or [],
        }

        missing_fields = []

        if not payload["name"]:
            missing_fields.append("name")
        if not payload["problem_type"]:
            missing_fields.append("problem_type")
        if not payload["problem_subtype"]:
            missing_fields.append("problem_subtype")
        if not payload["schedule_file_id"]:
            missing_fields.append("schedule_file_id")

        if missing_fields:
            return Response(
                {
                    "error": "O problema ainda não está pronto para ser enviado para o Java.",
                    "missing_fields": missing_fields,
                    "payload_preview": payload,
                },
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            java_response = requests.post(
                self.JAVA_BACKEND_URL,
                json=payload,
                timeout=60,
            )
        except requests.exceptions.RequestException as exc:
            return Response(
                {
                    "error": "Não foi possível contactar o backend Java.",
                    "details": str(exc),
                    "payload_preview": payload,
                },
                status=status.HTTP_502_BAD_GATEWAY
            )

        try:
            java_data = java_response.json()
        except ValueError:
            java_data = {"raw_response": java_response.text}

        if not java_response.ok:
            return Response(
                {
                    "error": "O backend Java respondeu com erro.",
                    "java_status_code": java_response.status_code,
                    "java_response": java_data,
                    "payload_preview": payload,
                },
                status=status.HTTP_502_BAD_GATEWAY
            )

        return Response(
            {
                "message": "Problema enviado com sucesso para o backend Java.",
                "payload_sent": payload,
                "java_response": java_data,
            },
            status=status.HTTP_200_OK
        )
