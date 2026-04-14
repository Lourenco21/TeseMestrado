from rest_framework import status, generics
from rest_framework.generics import ListAPIView
from rest_framework.parsers import MultiPartParser, FormParser
from rest_framework.response import Response
from rest_framework.views import APIView

from .problem_schemas import get_problem_schema, PROBLEM_SCHEMAS
from .problem_schemas.problem_catalog import PROBLEM_FAMILIES, CONSTRAINT_LIBRARY, OBJECTIVE_LIBRARY
from .serializers import ScheduleSerializer, ScheduleListSerializer, ProblemDraftSerializer
from .models import Schedule, ProblemDraft
from .services.file_reader import read_schedule_file, extract_columns_and_preview
from .services.column_matcher import match_canonical_fields_to_source_columns
from .services.schema import CANONICAL_FIELDS


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


class ScheduleMappingSaveView(APIView):
    def post(self, request, schedule_id, *args, **kwargs):
        try:
            schedule = Schedule.objects.get(pk=schedule_id)
        except Schedule.DoesNotExist:
            return Response({"error": "Schedule not found."}, status=status.HTTP_404_NOT_FOUND)

        mappings = request.data.get("mappings")

        if not isinstance(mappings, dict):
            return Response(
                {"error": "O campo 'mappings' tem de ser um objeto JSON."},
                status=status.HTTP_400_BAD_REQUEST
            )

        schedule.mapping_data = {
            "mappings": mappings,
        }
        schedule.save(update_fields=["mapping_data", "updated_at"])

        return Response({
            "message": "Mapping guardado com sucesso.",
            "mapping_data": schedule.mapping_data,
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
