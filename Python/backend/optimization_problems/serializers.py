from rest_framework import serializers
from .models import Schedule, ProblemDraft, RoomDataFile


class ScheduleSerializer(serializers.ModelSerializer):
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
    uploaded_schedule_name = serializers.CharField(
        source="uploaded_schedule.name",
        read_only=True
    )
    uploaded_schedule_file = serializers.FileField(
        source="uploaded_schedule.file",
        read_only=True
    )

    uploaded_rooms_file_name = serializers.CharField(
        source="uploaded_rooms_file.name",
        read_only=True
    )
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
            "selected_objectives",
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
