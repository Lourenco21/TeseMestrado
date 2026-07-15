from pathlib import Path

from django.core.validators import FileExtensionValidator
from django.db import models
from django.utils.timezone import now


# Create your models here.
class Schedule(models.Model):
    name = models.CharField(max_length=255, blank=True)
    file = models.FileField(upload_to='optimization_problems/solution/',
                            validators=[FileExtensionValidator(allowed_extensions=['csv', 'xlsx', 'xls'])])
    uploaded_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def save(self, *args, **kwargs):
        if (not self.name) and self.file and self.file.name:
            self.name = Path(self.file.name).name
        super().save(*args, **kwargs)


class ProblemDraft(models.Model):
    STATUS_CHOICES = [
        ("created", "Created"),
        ("problem_family_selected", "Problem family selected"),
        ("problem_subtype_selected", "Problem subtype selected"),
        ("file_uploaded", "File uploaded"),
        ("mapping_completed", "Mapping completed"),
        ("objectives_selected", "Objectives selected"),
        ("constraints_selected", "Constraints selected"),
        ("review_ready", "Review ready"),
        ("finalized", "Finalized"),
    ]

    name = models.CharField(max_length=200, blank=True, default="")
    status = models.CharField(max_length=50, choices=STATUS_CHOICES, default="created")
    current_step = models.PositiveIntegerField(default=1)

    uploaded_schedule = models.ForeignKey(
        "optimization_problems.Schedule",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="problem_drafts"
    )

    uploaded_rooms_file = models.ForeignKey(
        "optimization_problems.RoomDataFile",
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="problem_drafts"
    )

    problem_family = models.CharField(max_length=100, blank=True, default="")
    problem_subtype = models.CharField(max_length=100, blank=True, default="")

    mapping_data = models.JSONField(default=dict, blank=True)
    rooms_mapping_data = models.JSONField(default=dict, blank=True)
    
    room_feature_resolution = models.JSONField(default=dict, blank=True)

    selected_constraints = models.JSONField(default=list, blank=True)

    baseline_metrics = models.JSONField(default=dict, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.name or f"Problem Draft #{self.pk}"


class RoomDataFile(models.Model):
    name = models.CharField(max_length=200)
    file = models.FileField(
        upload_to="optimization_problems/rooms/",
        validators=[FileExtensionValidator(allowed_extensions=["csv", "xlsx", "xls"])]
    )
    uploaded_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.name


class Solution(models.Model):
    STATUS_CHOICES = [
        ("created", "Created"),
        ("running", "Running"),
        ("completed", "Completed"),
        ("failed", "Failed"),
    ]

    problem = models.ForeignKey(
        "optimization_problems.ProblemDraft",
        on_delete=models.CASCADE,
        related_name="solutions",
    )
    status = models.CharField(max_length=20, choices=STATUS_CHOICES, default="created")
    algorithm_used = models.CharField(max_length=100, blank=True, default="")
    used_parameters = models.JSONField(default=dict, blank=True)
    partition_type = models.CharField(max_length=50, blank=True, default="")
    reuse_solution = models.BooleanField(default=False)

    constraint_values = models.JSONField(default=dict, blank=True)
    penalty_summary = models.JSONField(default=dict, blank=True)
    partition_count = models.IntegerField(default=0)
    execution_time_seconds = models.FloatField(default=0.0)
    metrics = models.JSONField(default=dict, blank=True)

    schedule_file = models.FileField(
        upload_to="optimization_problems/solutions/",
        blank=True,
        null=True
    )

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"Solution #{self.pk} - Problem #{self.problem_id}"