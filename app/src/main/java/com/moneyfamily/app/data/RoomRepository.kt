package com.moneyfamily.app.data

import android.content.Context
import androidx.room.Room

class RoomRepository(context: Context) {
    private val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "moneyfamily.db")
        .addMigrations(MIGRATION_1_2)
        .build()
    private val dao = db.movementDao()
    private val types = db.typeDao()
    private val categories = db.categoryDao()
    private val members = db.familyMemberDao()
    private val mappings = db.typeCategoryDao()

    suspend fun all(): List<Movement> = dao.getAll().map { it.toModel() }
    suspend fun insert(item: Movement) = dao.insert(item.toEntity())
    suspend fun update(item: Movement) = dao.update(item.toEntity())
    suspend fun delete(item: Movement) = dao.delete(item.toEntity())

    suspend fun allTypes() = types.all()
    suspend fun activeTypes() = types.active()
    suspend fun addType(name: String) = types.insert(TypeEntity(name = name.trim()))
    suspend fun updateType(item: TypeEntity) = types.update(item)
    suspend fun setTypeActive(id: Long, active: Boolean) = types.setActive(id, active)

    suspend fun allCategories() = categories.all()
    suspend fun activeCategories() = categories.active()
    suspend fun addCategory(name: String) = categories.insert(CategoryEntity(name = name.trim()))
    suspend fun updateCategory(item: CategoryEntity) = categories.update(item)
    suspend fun setCategoryActive(id: Long, active: Boolean) = categories.setActive(id, active)

    suspend fun allMembers() = members.all()
    suspend fun activeMembers() = members.active()
    suspend fun addMember(name: String) = members.insert(FamilyMemberEntity(name = name.trim()))
    suspend fun updateMember(item: FamilyMemberEntity) = members.update(item)
    suspend fun setMemberActive(id: Long, active: Boolean) = members.setActive(id, active)

    suspend fun allMappings() = mappings.all()
    suspend fun categoryIdForType(typeId: Long) = mappings.all().firstOrNull { it.typeId == typeId }?.categoryId
    suspend fun setTypeCategory(typeId: Long, categoryId: Long) = mappings.upsert(TypeCategoryEntity(typeId = typeId, categoryId = categoryId))
    suspend fun removeTypeCategory(typeId: Long) = mappings.deleteForType(typeId)

    suspend fun seedDefaults() {
        val initial = linkedMapOf(
            "AMAZON" to "E-COMMERCE", "ASOS" to "E-COMMERCE", "NETFLIX" to "E-COMMERCE", "ZALANDO" to "E-COMMERCE", "SPOTIFY" to "E-COMMERCE", "PULL&BEAR" to "E-COMMERCE", "H&M" to "E-COMMERCE", "PRENATAL" to "E-COMMERCE", "DISNEYPLUS" to "E-COMMERCE", "VARIE" to "E-COMMERCE", "ABBIGLIAMENTO" to "E-COMMERCE", "WILLIAM HILL" to "E-COMMERCE", "ZARA" to "E-COMMERCE", "PLAYSTATION" to "E-COMMERCE", "BARBIERE" to "E-COMMERCE", "RICARICA TIM" to "E-COMMERCE",
            "SPESA" to "CASA", "UTENZE" to "CASA", "CONDOMINIO" to "CASA", "MANUTENZIONE" to "CASA", "AFFITTO" to "CASA", "VACANZA" to "CASA",
            "BENZINA" to "AUTO", "ASSICURAZIONE" to "AUTO", "BOLLO" to "AUTO", "ACCESSORI" to "AUTO", "RATA" to "AUTO", "TAGLIANDO" to "AUTO", "TELEPASS" to "AUTO", "AUTOLAVAGGIO" to "AUTO",
            "GINECOLOGO" to "MEDICO", "PEDIATRA" to "MEDICO", "ANALISI" to "MEDICO", "FARMACIA" to "MEDICO", "RISTORANTE" to "PRANZO/CENA", "CINEMA" to "PRANZO/CENA", "MATERNA" to "SCUOLA", "ELEMENTARI" to "SCUOLA", "NUOTO" to "SPORT", "GINNASTICA" to "SPORT", "DANZA" to "SPORT", "STIPENDIO" to "NTT DATA", "BUONI PASTO" to "NTT DATA", "TREDICESIMA" to "NTT DATA", "INPS" to "INPS", "730" to "AGENZIA ENTRATE"
        )
        initial.forEach { (typeName, categoryName) ->
            val typeId = types.insert(TypeEntity(name = typeName)).let { if (it == -1L) types.all().firstOrNull { t -> t.name == typeName }?.id ?: 0L else it }
            val categoryId = categories.insert(CategoryEntity(name = categoryName)).let { if (it == -1L) categories.all().firstOrNull { c -> c.name == categoryName }?.id ?: 0L else it }
            if (typeId > 0 && categoryId > 0) mappings.upsert(TypeCategoryEntity(typeId = typeId, categoryId = categoryId))
        }
        if (members.all().isEmpty()) listOf("Famiglia", "Papà", "Mamma", "Figlio 1", "Figlio 2").forEach { members.insert(FamilyMemberEntity(name = it)) }
    }

    fun close() = db.close()
}

private fun MovementEntity.toModel() = Movement(id, if (type == MovementType.INCOME.name) MovementType.INCOME else MovementType.EXPENSE, amount, category, description, date, member, paymentMethod)
private fun Movement.toEntity() = MovementEntity(id, type.name, amount, category, description, date, member, paymentMethod)
