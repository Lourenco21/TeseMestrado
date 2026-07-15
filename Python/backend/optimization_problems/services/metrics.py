"""
metrics.py
Serviço de cálculo de métricas de qualidade da solução, baseado nas
mesmas regras implementadas nas constraints Java (room_capacity_sufficiency,
room_exclusivity, capacity_waste, room_feature_mismatch,
consecutive_room_change, student_relocation).
"""
import re
import unicodedata
from collections import defaultdict
import datetime as dt
import pandas as pd

# ---------------------------------------------------------------------------
# Utilitários de normalização / mapeamento (já existentes no projeto)
# ---------------------------------------------------------------------------

def get_mapped_column(df, mapping, logical_key):
    column_name = (mapping or {}).get(logical_key)
    if not column_name:
        return None
    if column_name not in df.columns:
        return None
    return column_name


def split_feature_tokens(value):
    if value is None:
        return []
    text = str(value).strip()
    if not text:
        return []
    parts = re.split(r"[;,|]+", text)
    return [part.strip() for part in parts if part.strip()]


def normalize_text(value):
    if value is None:
        return ""
    text = str(value).strip().lower()
    if not text:
        return ""
    text = unicodedata.normalize("NFKD", text)
    text = "".join(char for char in text if not unicodedata.combining(char))
    text = text.replace("-", "_").replace("/", "_")
    text = re.sub(r"\s+", "_", text)
    text = re.sub(r"[^a-z0-9_]+", "", text)
    text = re.sub(r"_+", "_", text).strip("_")
    return text


# ---------------------------------------------------------------------------
# Extração de características das salas (3 formatos suportados)
# ---------------------------------------------------------------------------

def extract_room_characteristics(rooms_df, characteristics_config):
    """Devolve {row_index: set(nome_da_caracteristica)} para cada sala."""
    fmt = characteristics_config.get("format")
    cfg = characteristics_config.get("config", {})
    result = {}

    if fmt == "single_column_list":
        col = cfg.get("source_column")
        sep = cfg.get("separator", ",")
        for idx, row in rooms_df.iterrows():
            raw = str(row.get(col, "") or "")
            result[idx] = {v.strip() for v in raw.split(sep) if v.strip()}

    elif fmt == "multiple_columns":
        cols = cfg.get("selected_columns", [])
        marker_values = set(cfg.get("selected_values", []))
        for idx, row in rooms_df.iterrows():
            result[idx] = {
                c for c in cols
                if str(row.get(c, "")).strip() in marker_values
            }

    elif fmt == "range_columns":
        all_cols = list(rooms_df.columns)
        start_col = cfg.get("start_column")
        end_col = cfg.get("end_column")
        marker_values = set(cfg.get("selected_values", []))

        if start_col in all_cols and end_col in all_cols:
            start_i = all_cols.index(start_col)
            end_i = all_cols.index(end_col)
            range_cols = all_cols[start_i:end_i + 1]
        else:
            range_cols = []

        for idx, row in rooms_df.iterrows():
            result[idx] = {
                c for c in range_cols
                if str(row.get(c, "")).strip() in marker_values
            }

    return result


def build_rooms_lookup(rooms_df, rooms_mapping_data):
    """Devolve {normalized_room_name: {capacity, building, room_identity, features}}."""
    field_map = rooms_mapping_data.get("field_mappings", {})
    linking = rooms_mapping_data.get("linking", {})
    join_col = linking.get("rooms_file_room_column")

    char_map = extract_room_characteristics(
        rooms_df, rooms_mapping_data.get("characteristics", {})
    )

    lookup = {}
    for idx, row in rooms_df.iterrows():
        normalized_name = normalize_text(row.get(join_col))
        if not normalized_name:
            continue
        lookup[normalized_name] = {
            "capacity": row.get(field_map.get("capacity")),
            "building": row.get(field_map.get("building")),
            "room_identity": str(row.get(field_map.get("room_name"), "")).strip(),
            "features": char_map.get(idx, set()),
        }
    return lookup


# ---------------------------------------------------------------------------
# Resolução das características pedidas pela aula
# (corrigido: a key certa é "caracteristicas_pedidas_para_sala")
# ---------------------------------------------------------------------------

def apply_room_feature_resolution(df, mapping_data, room_feature_resolution):
    if df is None or df.empty:
        return df

    mapping = (mapping_data or {}).get("mapping", {}) or {}
    requested_column = get_mapped_column(
        df, mapping, "caracteristicas_pedidas_para_sala"
    )

    if not requested_column or requested_column not in df.columns:
        working_df = df.copy()
        working_df["_resolved_requested_room_features"] = [[] for _ in range(len(working_df))]
        return working_df

    resolution_items = (room_feature_resolution or {}).get("requested_values", []) or []

    resolution_by_normalized = {}
    for item in resolution_items:
        source_value = item.get("source_value")
        if not source_value:
            continue
        resolution_by_normalized[normalize_text(source_value)] = item

    def resolve_value(raw_value):
        tokens = split_feature_tokens(raw_value)
        if not tokens:
            return []

        resolved_tokens = []
        for token in tokens:
            normalized_token = normalize_text(token)
            resolution = resolution_by_normalized.get(normalized_token)

            if not resolution:
                resolved_tokens.append({
                    "source_value": token,
                    "resolution_type": "unresolved",
                    "target_values": [],
                })
                continue

            resolution_type = resolution.get("resolution_type", "unresolved")
            target_values = resolution.get("target_values", []) or []

            if resolution_type == "none_required":
                continue
            elif resolution_type == "map_to_room_feature":
                resolved_tokens.append({
                    "source_value": token,
                    "resolution_type": "map_to_room_feature",
                    "target_values": target_values,
                })
            elif resolution_type == "no_match":
                resolved_tokens.append({
                    "source_value": token,
                    "resolution_type": "no_match",
                    "target_values": [],
                })
            else:
                resolved_tokens.append({
                    "source_value": token,
                    "resolution_type": "unresolved",
                    "target_values": [],
                })

        return resolved_tokens

    working_df = df.copy()
    working_df["_resolved_requested_room_features"] = (
        working_df[requested_column].apply(resolve_value)
    )
    return working_df


# ---------------------------------------------------------------------------
# Construção da lista de eventos normalizados (equivalente a PreparedClassData
# + PreparedRoomData combinados)
# ---------------------------------------------------------------------------

def _to_float(value):
    try:
        if value is None or value == "":
            return None
        return float(value)
    except (TypeError, ValueError):
        return None


def _to_minutes(value):
    if value is None:
        return None
    if isinstance(value, (dt.time,)):
        return value.hour * 60 + value.minute
    if isinstance(value, (dt.datetime, pd.Timestamp)):
        return value.hour * 60 + value.minute
    text = str(value).strip()
    if not text:
        return None
    if " " in text:
        text = text.split(" ")[-1]  # descarta a parte da data, se existir
    parts = text.split(":")
    try:
        hours = int(parts[0])
        minutes = int(parts[1]) if len(parts) > 1 else 0
        return hours * 60 + minutes
    except (ValueError, IndexError):
        return None


def build_events(schedule_df, rooms_df, mapping_data, rooms_mapping_data,
                  room_feature_resolution):
    mapping = (mapping_data or {}).get("mapping", {}) or {}

    schedule_df = apply_room_feature_resolution(
        schedule_df, mapping_data, room_feature_resolution
    )

    rooms_lookup = build_rooms_lookup(rooms_df, rooms_mapping_data)
    schedule_room_col = (rooms_mapping_data or {}).get("linking", {}).get(
        "schedule_room_column"
    )
    # Fallback para o mapping do horário, caso o linking não esteja definido.
    if not schedule_room_col or schedule_room_col not in schedule_df.columns:
        schedule_room_col = mapping.get("sala")

    events = []
    for _, row in schedule_df.iterrows():
        raw_room = row.get(schedule_room_col, "")
        room_key = normalize_text(raw_room)
        room_info = rooms_lookup.get(room_key, {})

        requested = row.get("_resolved_requested_room_features", [])
        requested_sets = (
            [set(t["target_values"]) for t in requested] if requested else []
        )

        if pd.isna(raw_room) or str(raw_room).strip() == "":
            room_value = None
        else:
            room_value = room_info.get("room_identity") or str(raw_room).strip()

        events.append({
            "room": room_value,
            "capacity": _to_float(room_info.get("capacity")),
            "building": room_info.get("building"),
            "room_features": room_info.get("features", set()),
            "requested_features": requested_sets,
            "students": _to_float(row.get(mapping.get("numero_estudantes"))),
            "degree": row.get(mapping.get("curso")),
            "course": row.get(mapping.get("unidade_curricular")),
            "class_group": row.get(mapping.get("turma")),
            "shift": row.get(mapping.get("turno")),
            "day": row.get(mapping.get("dia")),
            "start": _to_minutes(row.get(mapping.get("hora_inicio"))),
            "end": _to_minutes(row.get(mapping.get("hora_fim"))),
        })

    return events


# ---------------------------------------------------------------------------
# Métricas (fórmulas espelhadas das constraints Java)
# ---------------------------------------------------------------------------

def metric_room_capacity_sufficiency(events):
    violations = sum(
        1.0 for e in events
        if e["students"] is not None and e["capacity"] is not None
        and e["students"] > e["capacity"]
    )
    return {"total_violations": violations, "affected_classes": int(violations)}


def metric_capacity_waste(events):
    total = 0.0
    for e in events:
        s, c = e["students"], e["capacity"]
        if s is None or c is None or s < 0 or c < 0:
            continue
        total += max(0.0, (c - s + 10))
    return {"total_waste": total}


def metric_room_feature_mismatch(events):
    affected_classes = 0
    total_missing_tokens = 0.0
    for e in events:
        normalized_room_features = {normalize_text(f) for f in e["room_features"]}
        class_has_mismatch = False
        for requirement in e["requested_features"]:
            if not requirement:
                continue
            normalized_requirement = {normalize_text(r) for r in requirement}
            if not (normalized_requirement & normalized_room_features):
                total_missing_tokens += 1.0
                class_has_mismatch = True
        if class_has_mismatch:
            affected_classes += 1
    return {
        "total_mismatches": total_missing_tokens,
        "affected_classes": affected_classes,
    }


def _group_and_penalize(events, group_key_fn, location_key):
    buckets = defaultdict(list)
    for e in events:
        gk = group_key_fn(e)
        if (
            gk is None
            or e["start"] is None
            or e["end"] is None
            or not e[location_key]
            or e["day"] is None
        ):
            continue
        buckets[(gk, e["day"])].append(e)

    total = 0.0
    changes = 0
    for slots in buckets.values():
        slots.sort(key=lambda ev: (ev["start"], ev["end"]))
        for a, b in zip(slots, slots[1:]):
            if a["end"] == b["start"] and a[location_key] != b[location_key]:
                total += 1.0
                changes += 1
    return total, changes


def metric_consecutive_room_change(events):
    def key(e):
        if not all([e["degree"], e["course"], e["class_group"], e["shift"]]):
            return None
        return (
            normalize_text(e["degree"]),
            normalize_text(e["course"]),
            normalize_text(e["class_group"]),
            normalize_text(e["shift"]),
        )

    events_with_room_key = [
        {**e, "room_identity_norm": normalize_text(e["room"])} for e in events
    ]
    total, changes = _group_and_penalize(events_with_room_key, key, "room_identity_norm")
    return {"total_penalty": total, "room_changes_detected": changes}


def metric_student_relocation(events):
    def key(e):
        if not all([e["degree"], e["class_group"]]):
            return None
        return (normalize_text(e["degree"]), normalize_text(e["class_group"]))

    events_with_building_key = [
        {**e, "building_norm": normalize_text(e["building"]) if e["building"] else None}
        for e in events
    ]
    total, changes = _group_and_penalize(
        events_with_building_key, key, "building_norm"
    )
    return {"total_penalty": total, "relocations_detected": changes}


def metric_room_exclusivity(events):
    """Deteta sobreposições: mesma sala, mesmo dia, endA > startB (par ordenado)."""
    groups = defaultdict(list)
    for e in events:
        if e["day"] is None or e["start"] is None or e["end"] is None or not e["room"]:
            continue
        groups[(normalize_text(e["room"]), e["day"])].append(e)

    total_overlaps = 0
    for slots in groups.values():
        sorted_slots = sorted(slots, key=lambda ev: (ev["start"], ev["end"]))
        for i in range(len(sorted_slots)):
            current = sorted_slots[i]
            for j in range(i + 1, len(sorted_slots)):
                nxt = sorted_slots[j]
                if nxt["start"] >= current["end"]:
                    break
                if current["start"] < nxt["end"] and nxt["start"] < current["end"]:
                    total_overlaps += 1

    return {"total_overlaps": total_overlaps}


METRIC_FUNCTIONS = {
    "room_capacity_sufficiency": metric_room_capacity_sufficiency,
    "room_exclusivity": metric_room_exclusivity,
    "capacity_waste": metric_capacity_waste,
    "room_feature_mismatch": metric_room_feature_mismatch,
    "consecutive_room_change": metric_consecutive_room_change,
    "student_relocation": metric_student_relocation,
}


def calculate_all_metrics(events, selected_constraints=None):
    """
    Calcula apenas as métricas correspondentes às restrições selecionadas
    no problema (selected_constraints), ou todas se None.
    """
    constraint_ids = set(selected_constraints) if selected_constraints else set(
        METRIC_FUNCTIONS.keys()
    )

    results = {}
    for constraint_id, fn in METRIC_FUNCTIONS.items():
        if constraint_id in constraint_ids:
            try:
                results[constraint_id] = fn(events)
            except Exception as exc:
                results[constraint_id] = {"error": str(exc)}

    return results

