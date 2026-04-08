from rest_framework import serializers
from .models import Schedule


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
