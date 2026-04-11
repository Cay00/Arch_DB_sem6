"""Dozwolone statusy zgłoszeń (wartości zapisane w bazie — po polsku)."""

ZGLOSZONE = "Zgłoszone"
ROZPATRYWANE = "Rozpatrywane"
ZAAKCEPTOWANE = "Zaakceptowane"
ODRZUCONE = "Odrzucone"

ALL_STATUSES = frozenset({ZGLOSZONE, ROZPATRYWANE, ZAAKCEPTOWANE, ODRZUCONE})

DEFAULT_ON_CREATE = ZGLOSZONE
