package com.tripnext.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun expenseCategory(value: String) = ExpenseCategory.valueOf(value)
    @TypeConverter fun expenseCategory(value: ExpenseCategory) = value.name
    @TypeConverter fun checklistCategory(value: String) = ChecklistCategory.valueOf(value)
    @TypeConverter fun checklistCategory(value: ChecklistCategory) = value.name
    @TypeConverter fun itineraryType(value: String) = ItineraryType.valueOf(value)
    @TypeConverter fun itineraryType(value: ItineraryType) = value.name
    @TypeConverter fun reservationType(value: String) = ReservationType.valueOf(value)
    @TypeConverter fun reservationType(value: ReservationType) = value.name
    @TypeConverter fun participantRole(value: String) = ParticipantRole.valueOf(value)
    @TypeConverter fun participantRole(value: ParticipantRole) = value.name
    @TypeConverter fun splitType(value: String) = SplitType.valueOf(value)
    @TypeConverter fun splitType(value: SplitType) = value.name
}
