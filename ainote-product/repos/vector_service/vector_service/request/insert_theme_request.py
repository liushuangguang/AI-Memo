from pydantic import BaseModel


class InsertThemeRequest(BaseModel):
    user_id: str
    theme: str
    theme_id: str
