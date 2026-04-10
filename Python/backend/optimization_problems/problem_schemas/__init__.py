from .university_course_timetabling import PROBLEM_SCHEMA as UCT_SCHEMA

PROBLEM_SCHEMAS = {
    UCT_SCHEMA["id"]: UCT_SCHEMA,
}


def get_problem_schema(subtype_id):
    return PROBLEM_SCHEMAS.get(subtype_id)