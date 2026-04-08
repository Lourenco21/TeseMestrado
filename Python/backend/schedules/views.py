from rest_framework import status
from rest_framework.generics import ListAPIView
from rest_framework.parsers import MultiPartParser, FormParser
from rest_framework.response import Response
from rest_framework.views import APIView
from .serializers import ScheduleSerializer, ScheduleListSerializer
from .models import Schedule
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


class ScheduleMappingSuggestionView(APIView):
    def get(self, request, schedule_id, *args, **kwargs):
        try:
            schedule = Schedule.objects.get(pk=schedule_id)
        except Schedule.DoesNotExist:
            return Response({"error": "Schedule not found."}, status=status.HTTP_404_NOT_FOUND)

        file_path = schedule.file.path

        try:
            df = read_schedule_file(file_path)
        except Exception as exc:
            return Response(
                {"error": f"Erro ao ler ficheiro: {str(exc)}"},
                status=status.HTTP_400_BAD_REQUEST
            )

        file_info = extract_columns_and_preview(df)
        matches = match_canonical_fields_to_source_columns(file_info["columns"])

        preview_lookup = {
            item["source_column"]: item["sample_values"]
            for item in file_info["preview"]
        }

        enriched_matches = []
        for match in matches:
            suggested_column = match["suggested_source_column"]

            enriched_matches.append({
                **match,
                "sample_values": preview_lookup.get(suggested_column, []) if suggested_column else [],
                "available_source_columns": file_info["columns"],
            })

        saved_mappings = {}
        if schedule.mapping_data and isinstance(schedule.mapping_data, dict):
            saved_mappings = schedule.mapping_data.get("mappings", {}) or {}

        if saved_mappings:
            selected_mappings = saved_mappings
            mode = "saved"
        else:
            selected_mappings = {
                match["target_field"]: match.get("suggested_source_column") or ""
                for match in enriched_matches
            }
            mode = "suggested"

        return Response({
            "schedule_id": schedule.id,
            "schedule_name": schedule.name,
            "source_columns": file_info["columns"],
            "matches": enriched_matches,
            "selected_mappings": selected_mappings,
            "mode": mode,
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
