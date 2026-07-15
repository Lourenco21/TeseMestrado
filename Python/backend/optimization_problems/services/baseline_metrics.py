from optimization_problems.services.file_reader import read_schedule_file
from optimization_problems.services.metrics import calculate_all_metrics, build_events
from optimization_problems.services.schedule_loading import load_schedule_dataframe


def calculate_baseline_metrics_for_draft(draft):
    """
    Calcula todas as métricas de qualidade (baseline) para um ProblemDraft,
    usando o schedule, rooms file e mapping já associados.
    Deve ser chamado apenas quando mapping_data, rooms_mapping_data e
    room_feature_resolution já estão completos (fim do wizard de mapping).
    """
    if not draft.uploaded_schedule or not draft.uploaded_rooms_file:
        return {}

    if not draft.mapping_data or not draft.rooms_mapping_data:
        return {}

    schedule_df = load_schedule_dataframe(draft.uploaded_schedule.file.path)
    rooms_df = read_schedule_file(draft.uploaded_rooms_file.file.path)

    events = build_events(
        schedule_df=schedule_df,
        rooms_df=rooms_df,
        mapping_data=draft.mapping_data,
        rooms_mapping_data=draft.rooms_mapping_data,
        room_feature_resolution=draft.room_feature_resolution,
    )

    return calculate_all_metrics(events, selected_constraints=None)
