from django.urls import path
from .views import ScheduleUploadView, ScheduleMappingSuggestionView, ScheduleMappingSaveView, ScheduleListView

urlpatterns = [
    path("", ScheduleListView.as_view(), name="schedule-list"),
    path('upload/', ScheduleUploadView.as_view(), name='schedule-upload'),
    path("<int:schedule_id>/mapping-suggestions/", ScheduleMappingSuggestionView.as_view(), name="schedule-mapping-suggestions"),
    path("<int:schedule_id>/save-mapping/", ScheduleMappingSaveView.as_view(), name="schedule-save-mapping"),
]