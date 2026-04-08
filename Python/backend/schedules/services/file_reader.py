import os
import pandas as pd


def try_read_csv(file_path):
    attempts = [
        {"sep": ",", "encoding": "utf-8"},
        {"sep": ";", "encoding": "utf-8"},
        {"sep": "\t", "encoding": "utf-8"},
        {"sep": ",", "encoding": "latin-1"},
        {"sep": ";", "encoding": "latin-1"},
        {"sep": "\t", "encoding": "latin-1"},
    ]

    errors = []

    for attempt in attempts:
        try:
            df = pd.read_csv(
                file_path,
                sep=attempt["sep"],
                encoding=attempt["encoding"],
                engine="python",
            )

            if df.shape[1] > 1:
                return df

        except Exception as exc:
            errors.append(f"{attempt} -> {str(exc)}")

    raise ValueError("Não foi possível ler o CSV. Tentativas: " + " | ".join(errors))


def read_schedule_file(file_path):
    extension = os.path.splitext(file_path)[1].lower()

    if extension == ".csv":
        return try_read_csv(file_path)

    if extension in [".xlsx", ".xls"]:
        return pd.read_excel(file_path)

    raise ValueError(f"Formato não suportado: {extension}")


def extract_columns_and_preview(df, preview_rows=5):
    columns = list(df.columns)
    preview = []

    for column in columns:
        values = df[column].dropna().astype(str).head(preview_rows).tolist()
        preview.append({
            "source_column": column,
            "sample_values": values,
        })

    return {
        "columns": columns,
        "preview": preview,
    }