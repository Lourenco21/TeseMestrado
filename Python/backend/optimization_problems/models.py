from django.core.validators import FileExtensionValidator
from django.db import models
from django.utils.timezone import now


# Create your models here.
class Schedule(models.Model):
    name = models.CharField(max_length=200)
    file = models.FileField(upload_to='optimization_problems/',
                            validators=[FileExtensionValidator(allowed_extensions=['csv', 'xlsx', 'xls'])])
    uploaded_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.name


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

    problem_family = models.CharField(max_length=100, blank=True, default="")
    problem_subtype = models.CharField(max_length=100, blank=True, default="")

    wizard_data = models.JSONField(default=dict, blank=True)

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return self.name or f"Problem Draft #{self.pk}"
