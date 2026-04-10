import re
from rapidfuzz import fuzz


def normalize_text(value):
    if value is None:
        return ""
    text = str(value).strip().lower()
    text = re.sub(r"[áàâãä]", "a", text)
    text = re.sub(r"[éèêë]", "e", text)
    text = re.sub(r"[íìîï]", "i", text)
    text = re.sub(r"[óòôõö]", "o", text)
    text = re.sub(r"[úùûü]", "u", text)
    text = re.sub(r"ç", "c", text)
    text = re.sub(r"[^a-z0-9_\s]", " ", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def fuzzy_similarity(a, b):
    a_norm = normalize_text(a)
    b_norm = normalize_text(b)

    if not a_norm or not b_norm:
        return 0

    return fuzz.ratio(a_norm, b_norm) / 100.0


def best_fuzzy_match(field_candidates, source_columns):
    best_column = ""
    best_score = 0.0

    for column in source_columns:
        for candidate in field_candidates:
            score = fuzzy_similarity(candidate, column)
            if score > best_score:
                best_score = score
                best_column = column

    return best_column, round(best_score, 3)


def match_canonical_fields_to_source_columns(source_columns, schema_fields, strong_threshold=0.75, weak_threshold=0.55):
    matches = []

    for field in schema_fields:
        field_key = field["key"]
        label = field.get("label", field_key)
        aliases = field.get("aliases", [])

        candidates = [field_key, label, *aliases]
        suggested_column, confidence = best_fuzzy_match(candidates, source_columns)

        if confidence >= strong_threshold:
            match_type = "strong"
        elif confidence >= weak_threshold:
            match_type = "weak"
        else:
            match_type = "none"
            suggested_column = ""

        matches.append({
            "target_field": field_key,
            "target_label": label,
            "suggested_source_column": suggested_column,
            "confidence": confidence,
            "match_type": match_type,
        })

    return matches