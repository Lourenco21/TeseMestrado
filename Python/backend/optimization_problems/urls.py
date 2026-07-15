from django.urls import path
from .views import ScheduleUploadView, ProblemMappingSuggestionsView, ScheduleListView, \
    ProblemDraftListCreateView, ProblemDraftDetailView, ProblemCatalogView, RoomDataFileUploadView, \
    ProblemRoomsFilePreviewView, ProblemRoomsMappingSaveView, ProblemRoomsMappingSuggestionsView, ProblemSendToJavaView, \
    ProblemRequestAlgorithmsView, ProblemExecuteView, FilesForJavaView, ProblemSolutionsListView, SolutionDetailView, \
    ProblemRoomFeatureResolutionAnalysisView, SolutionMetricsView

urlpatterns = [
    path("", ProblemDraftListCreateView.as_view(), name="problem-draft-list-create"),
    path("<int:pk>/", ProblemDraftDetailView.as_view(), name="problem-draft-detail"),
    path('upload/', ScheduleUploadView.as_view(), name='schedule-upload'),
    path("<int:problem_id>/mapping-suggestions/", ProblemMappingSuggestionsView.as_view(), name="problem-mapping-suggestions"),
    path("catalog/", ProblemCatalogView.as_view(), name="problem-catalog"),
    path("rooms/upload/", RoomDataFileUploadView.as_view(), name="rooms-upload"),
    path("<int:problem_id>/preview/", ProblemRoomsFilePreviewView.as_view(), name="problem-rooms-preview"),
    path("<int:problem_id>/rooms-mapping-save/", ProblemRoomsMappingSaveView.as_view(), name="problem-rooms-mapping-save"),
    path("<int:problem_id>/rooms-mapping-suggestions/", ProblemRoomsMappingSuggestionsView.as_view(), name="problem-rooms-mapping-suggestions"),
    path("<int:problem_id>/send-to-java/", ProblemSendToJavaView.as_view(), name="problem-send-to-java",),
    path("<int:problem_id>/request-algorithms/",ProblemRequestAlgorithmsView.as_view(),name="problem-request-algorithms",),
    path("<int:problem_id>/execute/", ProblemExecuteView.as_view(), name="problem-execute",),
    path("<int:problem_id>/files-for-java/",FilesForJavaView.as_view(),name="problem-data-for-java",),
    path("<int:problem_id>/solutions/", ProblemSolutionsListView.as_view(), name="problem-solutions-list"),
    path("<int:problem_id>/solutions/<int:solution_id>/", SolutionDetailView.as_view(), name="solution-detail"),
    path("<int:problem_id>/room-feature-resolution-analysis/", ProblemRoomFeatureResolutionAnalysisView.as_view(),name="problem-room-feature-resolution-analysis",),
    path("<int:problem_id>/solutions/<int:solution_id>/metrics/", SolutionMetricsView.as_view(), name="solution-metrics"),
]