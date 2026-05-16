PROBLEM_FAMILIES = [
    {
        "id": "scheduling",
        "label": "Scheduling",
        "description": "Distribuição de tarefas, eventos ou recursos ao longo do tempo.",
        "subtypes": [
            {"id": "workforce", "label": "Workforce Scheduling"},
            {"id": "project", "label": "Project Scheduling"},
            {"id": "appointment", "label": "Appointment Scheduling"},
        ],
    },
    {
        "id": "timetabling",
        "label": "Timetabling",
        "description": "Criação de horários para aulas, exames ou formação.",
        "subtypes": [
            {"id": "university_course_timetabling", "label": "University"},
            {"id": "school", "label": "School"},
            {"id": "exams", "label": "Exams"},
        ],
    },
    {
        "id": "allocation",
        "label": "Allocation",
        "description": "Atribuição de pessoas, recursos ou itens a entidades/slots.",
        "subtypes": [
            {"id": "staff", "label": "Staff Allocation"},
            {"id": "room", "label": "Room Allocation"},
            {"id": "asset", "label": "Asset Allocation"},
        ],
    },
]

OBJECTIVE_LIBRARY = {
    "timetabling": [
        {"id": "min_conflicts", "label": "Minimizar conflitos", "type": "hard_or_soft"},
        #{"id": "balance_load", "label": "Equilibrar carga", "type": "soft"},
        {"id": "maximize_preferences", "label": "Maximizar preferências", "type": "soft"},
    ],
    "scheduling": [
        {"id": "min_overtime", "label": "Minimizar horas extra", "type": "soft"},
        {"id": "respect_deadlines", "label": "Respeitar deadlines", "type": "hard_or_soft"},
    ],
    "allocation": [
        {"id": "min_unassigned", "label": "Minimizar não atribuídos", "type": "hard_or_soft"},
        {"id": "balance_utilization", "label": "Equilibrar utilização", "type": "soft"},
    ],
}

CONSTRAINT_LIBRARY = {
    "timetabling": [
        {
            "id": "room_capacity_sufficiency",
            "label": "Capacidade da sala suficiente",
            "description": "A sala atribuída deve ter capacidade igual ou superior ao número de estudantes inscritos."
        },
        {
            "id": "room_exclusivity",
            "label": "Sala não pode ter duas aulas ao mesmo tempo",
            "description": "Duas aulas que decorram no mesmo intervalo temporal não podem ser atribuídas à mesma sala."
        },
        {
            "id": "capacity_waste",
            "label": "Minimizar desperdício de capacidade",
            "description": "Deve ser reduzido o número de lugares sobrantes nas salas atribuídas, promovendo uma utilização mais eficiente do espaço."
        },
        {
            "id": "room_feature_mismatch",
            "label": "Adequação das características da sala",
            "description": "Atribuições a salas sem as características necessárias para a aula, como laboratório informático ou equipamento específico, devem ser penalizadas."
        },
{
            "id": "consecutive_room_change",
            "label": "Evitar mudança de sala em aulas consecutivas",
            "description": "Mudanças desnecessárias de sala entre aulas seguidas da mesma unidade curricular devem ser desencorajadas."
        },
        {
            "id": "student_relocation",
            "label": "Minimizar deslocação dos estudantes",
            "description": "Deve ser reduzida a deslocação entre edifícios ou salas em aulas consecutivas do mesmo contexto académico."
        },
    ],
    "scheduling": [
        {
            "id": "resource_availability",
            "label": "Disponibilidade do recurso",
            "hard": True,
            "requires_additional_data": True,
            "additional_data_requirements": [
                {
                    "id": "disponibilidade_recurso",
                    "label": "Disponibilidade do recurso",
                    "entity": "recurso",
                    "accepted_sources": ["mesmo_ficheiro", "ficheiro_separado"],
                }
            ],
        },
        {
            "id": "precedence",
            "label": "Respeitar precedências",
            "hard": True,
            "requires_additional_data": False,
            "additional_data_requirements": [],
        },
    ],
    "allocation": [
        {
            "id": "unique_assignment",
            "label": "Atribuição única",
            "hard": True,
            "requires_additional_data": False,
            "additional_data_requirements": [],
        },
        {
            "id": "capacity_limit",
            "label": "Limite de capacidade",
            "hard": True,
            "requires_additional_data": False,
            "additional_data_requirements": [],
        },
    ],
}