from django.urls import path
from .views import ScheduleUploadView, ProblemMappingSuggestionsView, ScheduleMappingSaveView, ScheduleListView, \
    ProblemDraftListCreateView, ProblemDraftDetailView, ProblemCatalogView

urlpatterns = [
    path("", ProblemDraftListCreateView.as_view(), name="problem-draft-list-create"),
    path("<int:pk>/", ProblemDraftDetailView.as_view(), name="problem-draft-detail"),
    path('upload/', ScheduleUploadView.as_view(), name='schedule-upload'),
    path("<int:problem_id>/mapping-suggestions/", ProblemMappingSuggestionsView.as_view(), name="problem-mapping-suggestions"),
    path("<int:schedule_id>/save-mapping/", ScheduleMappingSaveView.as_view(), name="schedule-save-mapping"),
    path("catalog/", ProblemCatalogView.as_view(), name="problem-catalog"),
]