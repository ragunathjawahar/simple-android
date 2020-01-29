package org.simple.clinic.bloodsugar

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.simple.clinic.TestClinicApp
import org.simple.clinic.TestData
import org.simple.clinic.patient.SyncStatus
import org.simple.clinic.rules.LocalAuthenticationRule
import org.simple.clinic.storage.Timestamps
import org.simple.clinic.util.RxErrorsRule
import org.simple.clinic.util.TestUtcClock
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.temporal.ChronoUnit
import org.threeten.bp.temporal.ChronoUnit.DAYS
import java.util.UUID
import javax.inject.Inject

class BloodSugarRepositoryAndroidTest {

  @Inject
  lateinit var repository: BloodSugarRepository

  @Inject
  lateinit var clock: TestUtcClock

  @Inject
  lateinit var appDatabase: org.simple.clinic.AppDatabase

  @Inject
  lateinit var testData: TestData

  private val authenticationRule = LocalAuthenticationRule()

  private val rxErrorsRule = RxErrorsRule()

  @get:Rule
  val ruleChain = RuleChain
      .outerRule(authenticationRule)
      .around(rxErrorsRule)!!

  @Before
  fun setUp() {
    TestClinicApp.appComponent().inject(this)
    clock.setDate(LocalDate.of(2000, Month.JANUARY, 1))
  }

  @Test
  fun saving_a_blood_sugar_reading_should_work_correctly() {
    // given
    val bloodSugarUuid = UUID.fromString("e35dea7a-b13b-4ab1-aaa4-0c5998e5c79f")
    val bloodSugarReading = BloodSugarReading(value = 50, type = Random)
    val now = Instant.now(clock)
    val patientUuid = UUID.fromString("a5921ec9-5c70-421a-bb0b-1291364683f6")
    val facility = testData.qaFacility()
    val user = testData.qaUser()

    val expectedBloodSugarMeasurement = BloodSugarMeasurement(
        uuid = bloodSugarUuid,
        reading = bloodSugarReading,
        recordedAt = now,
        patientUuid = patientUuid,
        userUuid = user.uuid,
        facilityUuid = facility.uuid,
        timestamps = Timestamps.create(clock),
        syncStatus = SyncStatus.PENDING
    )

    // when
    val savedBloodSugar = repository.saveMeasurement(
        uuid = bloodSugarUuid,
        reading = bloodSugarReading,
        patientUuid = patientUuid,
        loggedInUser = user,
        facility = facility,
        recordedAt = now)
        .blockingGet()

    // then
    assertThat(savedBloodSugar).isEqualTo(expectedBloodSugarMeasurement)
  }

  @Test
  fun fetching_latest_blood_sugars_should_return_a_list_of_measurements_sorted_by_recordedAt() {
    //given
    val patientUuid = UUID.fromString("19848a57-496a-46d6-aa5c-94d35e3b4139")
    val bloodSugarToday = testData.bloodSugarMeasurement(UUID.fromString("290b751b-7c6f-4a40-9f00-532170dab252"), recordedAt = Instant.now(clock), patientUuid = patientUuid)
    val bloodSugarYesterday = testData.bloodSugarMeasurement(UUID.fromString("060aac7a-265f-4b94-9253-85a382a42a8d"), recordedAt = Instant.now(clock).minus(1, ChronoUnit.DAYS), patientUuid = patientUuid)
    val bloodSugarTomorrow = testData.bloodSugarMeasurement(UUID.fromString("ae6534a6-e967-45d5-8b3e-4c472fea8b51"), recordedAt = Instant.now(clock).plus(1, ChronoUnit.DAYS), patientUuid = patientUuid)

    val expected = listOf(bloodSugarTomorrow, bloodSugarToday, bloodSugarYesterday)

    appDatabase.bloodSugarDao().save(listOf(bloodSugarToday, bloodSugarYesterday, bloodSugarTomorrow))

    //when
    val bloodSugars = repository.latestMeasurements(patientUuid, 10).blockingFirst()

    //then
    assertThat(bloodSugars).isEqualTo(expected)
  }

  @Test
  fun when_fetching_all_blood_sugars_the_list_should_be_ordered_by_recorded_at() {
    val patientUuid = UUID.fromString("da8317c6-cc8f-44bc-81c7-964c5207cd0c")
    val bloodSugarRecordRightNow = testData.bloodSugarMeasurement(
        uuid = UUID.fromString("a3c08653-93f5-48b7-800c-3e298590ede8"),
        patientUuid = patientUuid,
        recordedAt = Instant.now(clock)
    )
    val bloodSugarRecordADayInFuture = testData.bloodSugarMeasurement(
        uuid = UUID.fromString("0dcb44b4-aa71-4b98-be7c-2548c91110d6"),
        patientUuid = patientUuid,
        recordedAt = Instant.now(clock).plus(1, DAYS)
    )
    val bloodSugarADayInPast = testData.bloodSugarMeasurement(
        uuid = UUID.fromString("ab383fdf-1aa9-4fc8-ba8e-bec1dcb92d16"),
        patientUuid = patientUuid,
        recordedAt = Instant.now(clock).minus(1, DAYS)
    )
    val bloodSugarDeleted = testData.bloodSugarMeasurement(
        uuid = UUID.fromString("ea1b6347-6ac7-4db0-a0c7-f823ef585f5f"),
        patientUuid = patientUuid,
        recordedAt = Instant.now(clock).minus(2, DAYS),
        deletedAt = Instant.now(clock).minus(1, DAYS)
    )

    appDatabase.bloodSugarDao().save(listOf(bloodSugarRecordRightNow, bloodSugarRecordADayInFuture, bloodSugarADayInPast, bloodSugarDeleted))

    val bloodSugarMeasurements = repository.allBloodSugars(patientUuid).blockingFirst()

    assertThat(bloodSugarMeasurements)
        .isEqualTo(listOf(
            bloodSugarRecordADayInFuture,
            bloodSugarRecordRightNow,
            bloodSugarADayInPast
        ))
  }
}
