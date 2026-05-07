ROOMS_FILE_SCHEMA = {
    "id": "rooms_file_mapping",
    "label": "Ficheiro de salas",
    "description": "Schema para mapping do ficheiro adicional com dados das salas.",
    "fields": [
        {
            "key": "room_name",
            "label": "Sala",
            "description": "Identificador ou nome da sala no ficheiro adicional.",
            "required": True,
            "data_type": "string",
            "aliases": ["sala", "room", "nome sala", "nome_sala", "classroom"],
        },
        {
            "key": "capacity",
            "label": "Número de lugares",
            "description": "Capacidade ou lotação da sala.",
            "required": False,
            "data_type": "number",
            "aliases": ["capacidade", "lotacao", "lotação", "lugares", "capacity", "seats"],
        },
        {
            "key": "building",
            "label": "Edifício",
            "description": "Edifício onde a sala se encontra.",
            "required": False,
            "data_type": "string",
            "aliases": ["edificio", "edifício", "building", "bloco"],
        },
    ],
    "characteristics": {
        "key": "characteristics_of_room",
        "label": "Características da sala",
        "description": "Forma como as características da sala estão representadas no ficheiro adicional.",
        "required": False,
        "supported_formats": [
            "single_column_list",
            "multiple_flag_columns",
            "range_flag_columns",
        ],
        "default_active_values": ["1", "X", "Sim", "TRUE"],
        "default_separators": [";", ",", "|"],
    },
}