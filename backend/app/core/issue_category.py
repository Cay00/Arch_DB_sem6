ALLOWED_CATEGORIES = (
    "Drogi",
    "Zieleń",
    "Wandalizm",
    "Oświetlenie",
    "Inwestycje",
    "Porządek",
)

_NORMALIZATION_MAP = {
    "drogi": "Drogi",
    "droga": "Drogi",
    "zielen": "Zieleń",
    "zieleń": "Zieleń",
    "wandalizm": "Wandalizm",
    "akt wandalizmu": "Wandalizm",
    "oswietlenie": "Oświetlenie",
    "oświetlenie": "Oświetlenie",
    "inwestycje": "Inwestycje",
    "inwestycja": "Inwestycje",
    "porzadek": "Porządek",
    "porządek": "Porządek",
}


def normalize_issue_category(value: str) -> str:
    key = value.strip().lower()
    return _NORMALIZATION_MAP.get(key, value.strip())
