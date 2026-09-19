from pydantic import BaseModel


class SameThemeNotesRequest(BaseModel):
    user_id: str
    theme_name: str
    theme_content: str
