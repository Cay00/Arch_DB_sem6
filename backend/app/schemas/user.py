from typing import Literal

from pydantic import BaseModel, ConfigDict, EmailStr

UserRole = Literal["Citizen", "Official"]


class UserCreate(BaseModel):
    email: EmailStr
    password_hash: str
    role: UserRole = "Citizen"


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    role: UserRole
