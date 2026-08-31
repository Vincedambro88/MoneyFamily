package com.moneyfamily.app.data

import android.app.Activity
import android.content.Context
import androidx.room.Room

class RoomRepository(private val context: Context) {
    private val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "moneyfamily.db")
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    private val dao = db.movementDao()
    private val types = db.typeDao()
    private val categories = db.categoryDao()
    private val members = db.familyMemberDao()
    private val mappings = db.typeCategoryDao()

    suspend fun all(): List<Movement> = dao.getAll().map { it.toModel() }
    suspend fun insert(item: Movement) {
        dao.insert(item.toEntity())
        if (ImportRefresh.consume()) {
            (context as? Activity)?.runOnUiThread {
                it.postDelayed({ (context as? Activity)?.recreate() }, 250L)
            }
        }
    }
    suspend fun update(item: Movement) = dao.update(item.toEntity())
    suspend fun delete(item: Movement) = dao.delete(item.toEntity())

    // Configuration screens only expose active master data. Deactivation is the
    // safe delete semantics because historical movements keep their stored names.
    suspend fun allTypes(): List<TypeEntity> { seedDefaults(); return types.active() }
    suspend fun activeTypes(): List<TypeEntity> { seedDefaults(); return types.active() }
    suspend fun addType(name: String) = types.insert(TypeEntity(name = name.trim()))
    suspend fun updateType(item: TypeEntity) = types.update(item)
    suspend fun setTypeActive(id: Long, active: Boolean) = types.setActive(id, active)

    suspend fun allCategories(): List<CategoryEntity> { seedDefaults(); return categories.active() }
    suspend fun activeCategories(): List<CategoryEntity> { seedDefaults(); return categories.active() }
    suspend fun addCategory(name: String) = categories.insert(CategoryEntity(name = name.trim()))
    suspend fun updateCategory(item: CategoryEntity) = categories.update(item)
    suspend fun setCategoryActive(id: Long, active: Boolean) = categories.setActive(id, active)

    suspend fun allMembers(): List<FamilyMemberEntity> { seedDefaults(); return members.active() }
    suspend fun activeMembers(): List<FamilyMemberEntity> { seedDefaults(); return members.active() }
    suspend fun addMember(name: String) = members.insert(FamilyMemberEntity(name = name.trim()))
    suspend fun updateMember(item: FamilyMemberEntity) = members.update(item)
    suspend fun setMemberActive(id: Long, active: Boolean) = members.setActive(id, active)

    suspend fun allMappings(): List<TypeCategoryEntity> { seedDefaults(); return mappings.all() }
    suspend fun categoryIdForType(typeId: Long): Long? { seedDefaults(); return mappings.all().firstOrNull { it.typeId == typeId }?.categoryId }
    suspend fun setTypeCategory(typeId: Long, categoryId: Long) = mappings.upsert(TypeCategoryEntity(typeId = typeId, categoryId = categoryId))
    suspend fun removeTypeCategory(typeId: Long) = mappings.deleteForType(typeId)

    suspend fun seedDefaults() {
        val existingNttCategory = categories.all().firstOrNull { it.name.equals("NTT DATA", true) }
        val existingWorkCategory = categories.all().firstOrNull { it.name.equals("LAVORO", true) }
        if (existingNttCategory != null && existingWorkCategory == null) categories.update(existingNttCategory.copy(name = "LAVORO"))
        val defaults = listOf(
            "AMAZON" to "E-COMMERCE", "ASOS" to "E-COMMERCE", "NETFLIX" to "E-COMMERCE", "ZALANDO" to "E-COMMERCE",
            "SPOTIFY" to "E-COMMERCE", "PULL&BEAR" to "E-COMMERCE", "H&M" to "E-COMMERCE", "PRENATAL" to "E-COMMERCE",
            "DISNEYPLUS" to "E-COMMERCE", "VARIE" to "E-COMMERCE", "ABBIGLIAMENTO" to "E-COMMERCE", "WILLIAM HILL" to "E-COMMERCE",
            "ZARA" to "E-COMMERCE", "PLAYSTATION" to "E-COMMERCE", "BARBIERE" to "E-COMMERCE", "RICARICA TIM" to "E-COMMERCE",
            "SPESA" to "CASA", "UTENZE" to "CASA", "CONDOMINIO" to "CASA", "MANUTENZIONE" to "CASA", "AFFITTO" to "CASA", "VACANZA" to "CASA",
            "BENZINA" to "AUTO", "ASSICURAZIONE" to "AUTO", "BOLLO" to "AUTO", "ACCESSORI" to "AUTO", "RATA" to "AUTO", "TAGLIANDO" to "AUTO", "TELEPASS" to "AUTO", "AUTOLAVAGGIO" to "AUTO",
            "GINECOLOGO" to "MEDICO", "PEDIATRA" to "MEDICO", "ANALISI" to "MEDICO", "FARMACIA" to "MEDICO",
            "RISTORANTE" to "PRANZO/CENA", "CINEMA" to "PRANZO/CENA",
            "MATERNA" to "SCUOLA", "ELEMENTARI" to "SCUOLA",
            "NUOTO" to "SPORT", "GINNASTICA" to "SPORT", "DANZA" to "SPORT",
            "STIPENDIO" to "LAVORO", "BUONI PASTO" to "LAVORO", "TREDICESIMA" to "LAVORO",
            "INPS" to "INPS", "730" to "AGENZIA ENTRATE"
        )
        val typeByName = types.all().associateBy { it.name }.toMutableMap()
        val categoryByName = categories.all().associateBy { it.name }.toMutableMap()
        val mappingByType = mappings.all().associateBy { it.typeId }.toMutableMap()
        defaults.forEach { (typeName, categoryName) ->
            val typeId = typeByName[typeName]?.id ?: types.insert(TypeEntity(name = typeName)).also { insertedId -> require(insertedId > 0L) { "Unable to seed type $typeName" }; typeByName[typeName] = TypeEntity(id = insertedId, name = typeName) }
            val categoryId = categoryByName[categoryName]?.id ?: categories.insert(CategoryEntity(name = categoryName)).also { insertedId -> require(insertedId > 0L) { "Unable to seed category $categoryName" }; categoryByName[categoryName] = CategoryEntity(id = insertedId, name = categoryName) }
            if (!mappingByType.containsKey(typeId)) { val mapping = TypeCategoryEntity(typeId = typeId, categoryId = categoryId); mappings.upsert(mapping); mappingByType[typeId] = mapping }
        }
        if (members.all().isEmpty()) listOf("Famiglia", "Papà", "Mamma", "Figlio 1", "Figlio 2").forEach { members.insert(FamilyMemberEntity(name = it)) }
    }

    fun close() = db.close()
}

private fun MovementEntity.toModel() = Movement(id, if (type == MovementType.INCOME.name) MovementType.INCOME else MovementType.EXPENSE, amount, category, description, date, member, paymentMethod, typeName)
private fun Movement.toEntity() = MovementEntity(id, type.name, typeName, amount, category, description, date, member, paymentMethod)
