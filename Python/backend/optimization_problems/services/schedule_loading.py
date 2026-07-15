import pandas as pd


def load_schedule_dataframe(file_path):
    lower_path = file_path.lower()

    if lower_path.endswith(".csv"):
        attempts = [
            {"encoding": "utf-8", "sep": ","},
            {"encoding": "utf-8-sig", "sep": ","},
            {"encoding": "latin1", "sep": ","},
            {"encoding": "utf-8", "sep": ";"},
            {"encoding": "utf-8-sig", "sep": ";"},
            {"encoding": "latin1", "sep": ";"},
        ]

        last_error = None

        for attempt in attempts:
            try:
                df = pd.read_csv(
                    file_path,
                    encoding=attempt["encoding"],
                    sep=attempt["sep"],
                    engine="python",
                )
                if df.shape[1] > 1:
                    return df
            except Exception as exc:
                last_error = exc

        raise ValueError(f"Não foi possível ler o CSV. Último erro: {last_error}")

    if lower_path.endswith(".xlsx"):
        try:
            return pd.read_excel(file_path, engine="openpyxl")
        except Exception as exc:
            raise ValueError(f"Erro ao ler XLSX: {exc}")

    if lower_path.endswith(".xls"):
        try:
            return pd.read_excel(file_path)
        except Exception as exc:
            raise ValueError(f"Erro ao ler XLS: {exc}")

    raise ValueError("Formato de ficheiro não suportado.")
