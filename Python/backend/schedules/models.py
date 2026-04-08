from django.core.validators import FileExtensionValidator
from django.db import models
from django.utils.timezone import now


# Create your models here.
class Schedule(models.Model):
    name = models.CharField(max_length=200)
    file = models.FileField(upload_to='schedules/',
                            validators=[FileExtensionValidator(allowed_extensions=['csv', 'xlsx', 'xls'])])
    uploaded_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    mapping_data = models.JSONField(null=True, blank=True)

    def __str__(self):
        return self.name
