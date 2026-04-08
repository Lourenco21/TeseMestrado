import re
import unicodedata
from rapidfuzz import fuzz, process

from .schema import CANONICAL_FIELDS


def normalize_text(value):
    if not value:
        return ""

    value = str(value).strip().lower()
    value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("utf-8")
    value = re.sub(r"[_\-]+", " ", value)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def confidence_label(score):
    if score >= 85:
        return "high"
    if score >= 70:
        return "medium"
    return "low"


def match_canonical_fields_to_source_columns(source_columns):
    normalized_source_columns = {
        column: normalize_text(column)
        for column in source_columns
    }

    source_choices = list(normalized_source_columns.values())
    reverse_lookup = {v: k for k, v in normalized_source_columns.items()}

    results = []

    for field_key, meta in CANONICAL_FIELDS.items():
        target_label = meta["label"]
        normalized_target = normalize_text(target_label)

        match = process.extractOne(
            normalized_target,
            source_choices,
            scorer=fuzz.WRatio,
        )

        if match:
            matched_normalized_column, score, _ = match
            suggested_source_column = reverse_lookup[matched_normalized_column]
        else:
            suggested_source_column = None
            score = 0

        results.append({
            "target_field": field_key,
            "target_label": target_label,
            "required": meta["required"],
            "suggested_source_column": suggested_source_column,
            "score": round(score, 2),
            "confidence": confidence_label(score),
        })

    return results