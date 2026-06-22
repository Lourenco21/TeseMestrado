from pathlib import Path

from rest_framework import serializers
from .models import Schedule, ProblemDraft, RoomDataFile, Solution


class ScheduleSerializer(serializers.ModelSerializer):
    name = serializers.CharField(source="display_name", read_only=True)
    class Meta:
        model = Schedule
        fields = ['id', 'name', 'file', 'uploaded_at', 'updated_at']
        read_only_fields = ['id', 'uploaded_at', 'updated_at']


class ScheduleListSerializer(serializers.ModelSerializer):
    file_name = serializers.SerializerMethodField()
    has_mapping = serializers.SerializerMethodField()

    class Meta:
        model = Schedule
        fields = [
            "id",
            "name",
            "file",
            "file_name",
            "uploaded_at",
            "updated_at",
        ]

    def get_file_name(self, obj):
        if obj.file:
            return obj.file.name.split("/")[-1]
        return ""


class RoomDataFileSerializer(serializers.ModelSerializer):
    class Meta:
        model = RoomDataFile
        fields = ["id", "name", "file", "uploaded_at", "updated_at"]
        read_only_fields = ["id", "uploaded_at", "updated_at"]


class ProblemDraftSerializer(serializers.ModelSerializer):
    uploaded_schedule_name = serializers.SerializerMethodField()
    uploaded_schedule_file = serializers.FileField(
        source="uploaded_schedule.file",
        read_only=True
    )

    uploaded_rooms_file_name = serializers.SerializerMethodField()
    uploaded_rooms_file_file = serializers.FileField(
        source="uploaded_rooms_file.file",
        read_only=True
    )

    class Meta:
        model = ProblemDraft
        fields = [
            "id",
            "name",
            "status",
            "current_step",
            "problem_family",
            "problem_subtype",
            "uploaded_schedule",
            "uploaded_schedule_name",
            "uploaded_schedule_file",
            "uploaded_rooms_file",
            "uploaded_rooms_file_name",
            "uploaded_rooms_file_file",
            "mapping_data",
            "rooms_mapping_data",
            "selected_constraints",
            "created_at",
            "updated_at",
        ]
        read_only_fields = [
            "id",
            "created_at",
            "updated_at",
            "uploaded_schedule_name",
            "uploaded_schedule_file",
            "uploaded_rooms_file_name",
            "uploaded_rooms_file_file",
        ]

    def get_uploaded_schedule_name(self, obj):
        schedule = getattr(obj, "uploaded_schedule", None)
        if not schedule:
            return None
        if getattr(schedule, "name", None):
            return schedule.name
        if getattr(schedule, "file", None) and schedule.file.name:
            return Path(schedule.file.name).name
        return None

    def get_uploaded_rooms_file_name(self, obj):
        rooms_file = getattr(obj, "uploaded_rooms_file", None)
        if not rooms_file:
            return None
        if getattr(rooms_file, "name", None):
            return rooms_file.name
        if getattr(rooms_file, "file", None) and rooms_file.file.name:
            return Path(rooms_file.file.name).name
        return None


class SolutionListSerializer(serializers.ModelSerializer):
    class Meta:
        model = Solution
        fields = [
            "id",
            "status",
            "algorithm_used",
            "partition_type",
            "reuse_solution",
            "created_at",
            "updated_at",
        ]


class SolutionDetailSerializer(serializers.ModelSerializer):
    schedule_file_url = serializers.SerializerMethodField()

    class Meta:
        model = Solution
        fields = [
            "id",
            "problem",
            "status",
            "algorithm_used",
            "used_parameters",
            "partition_type",
            "reuse_solution",
            "constraint_values",
            "penalty_summary",
            "execution_result",
            "partition_count",
            "schedule_file",
            "schedule_file_url",
            "created_at",
            "updated_at",
        ]

    def get_schedule_file_url(self, obj):
        request = self.context.get("request")
        if not obj.schedule_file:
            return None

        url = obj.schedule_file.url
        return request.build_absolute_uri(url) if request else url
