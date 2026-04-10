from rest_framework import serializers
from .models import Schedule, ProblemDraft


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
            "has_mapping",
        ]

    def get_file_name(self, obj):
        if obj.file:
            return obj.file.name.split("/")[-1]
        return ""

    def get_has_mapping(self, obj):
        return bool(obj.mapping_data and obj.mapping_data.get("mappings"))


class ProblemDraftSerializer(serializers.ModelSerializer):
    uploaded_schedule_name = serializers.CharField(
        source="uploaded_schedule.name",
        read_only=True
    )
    uploaded_schedule_file = serializers.FileField(
        source="uploaded_schedule.file",
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
            "wizard_data",
            "created_at",
            "updated_at",
        ]
        read_only_fields = [
            "id",
            "created_at",
            "updated_at",
            "uploaded_schedule_name",
            "uploaded_schedule_file",
        ]
