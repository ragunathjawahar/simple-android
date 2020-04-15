package org.simple.clinic.contactpatient

import com.spotify.mobius.test.NextMatchers.hasEffects
import com.spotify.mobius.test.NextMatchers.hasModel
import com.spotify.mobius.test.NextMatchers.hasNoEffects
import com.spotify.mobius.test.NextMatchers.hasNoModel
import com.spotify.mobius.test.UpdateSpec
import com.spotify.mobius.test.UpdateSpec.assertThatNext
import org.junit.Test
import org.simple.clinic.TestData
import org.simple.clinic.overdue.AppointmentConfig
import org.simple.clinic.overdue.PotentialAppointmentDate
import org.simple.clinic.overdue.TimeToAppointment
import org.simple.clinic.overdue.TimeToAppointment.Days
import org.simple.clinic.overdue.TimeToAppointment.Weeks
import org.simple.clinic.phone.PhoneNumberMaskerConfig
import org.simple.clinic.util.Just
import org.simple.clinic.util.RuntimePermissionResult.DENIED
import org.simple.clinic.util.RuntimePermissionResult.GRANTED
import org.simple.clinic.util.TestUserClock
import org.threeten.bp.LocalDate
import org.threeten.bp.Period
import java.util.UUID

class ContactPatientUpdateTest {

  private val patientUuid = UUID.fromString("b5eccb67-6425-4d48-9c17-65e9b267f9eb")
  private val patientPhoneNumber = "1234567890"
  private val patientProfile = TestData.patientProfile(
      patientUuid = patientUuid,
      patientPhoneNumber = patientPhoneNumber
  )
  private val overdueAppointment = TestData.overdueAppointment(
      patientUuid = patientUuid,
      facilityUuid = UUID.fromString("c97a8b30-8094-4c93-9ad6-ecc100130943"),
      phoneNumber = patientProfile.phoneNumbers.first(),
      appointmentUuid = UUID.fromString("f1b11fa6-3622-4f82-b74b-dd08dd563f1a"),
      gender = patientProfile.patient.gender,
      age = patientProfile.patient.age,
      dateOfBirth = patientProfile.patient.dateOfBirth
  )
  private val proxyPhoneNumberForSecureCalls = "9999988888"
  private val timeToAppointments = listOf(
      Days(1),
      Weeks(1),
      Weeks(2)
  )
  private val clock = TestUserClock(LocalDate.parse("2018-01-01"))

  private val spec = UpdateSpec(ContactPatientUpdate(proxyPhoneNumberForMaskedCalls = proxyPhoneNumberForSecureCalls))

  @Test
  fun `when the patient profile is loaded, the ui must be updated`() {
    val defaultModel = defaultModel()

    spec
        .given(defaultModel)
        .whenEvent(PatientProfileLoaded(patientProfile))
        .then(assertThatNext(
            hasModel(defaultModel.patientProfileLoaded(patientProfile)),
            hasNoEffects()
        ))
  }

  @Test
  fun `when the overdue appointment is loaded, the ui must be updated`() {
    val appointment = Just(overdueAppointment)
    val defaultModel = defaultModel()

    spec
        .given(defaultModel)
        .whenEvent(OverdueAppointmentLoaded(appointment))
        .then(assertThatNext(
            hasModel(defaultModel.overdueAppointmentLoaded(appointment))
        ))
  }

  @Test
  fun `when normal call is selected and the call permission is granted, directly call the patient with auto dial`() {
    val model = defaultModel()
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))

    spec
        .given(model)
        .whenEvent(NormalCallClicked(permission = Just(GRANTED)))
        .then(assertThatNext(
            hasNoModel(),
            hasEffects(DirectCallWithAutomaticDialer(patientPhoneNumber) as ContactPatientEffect)
        ))
  }

  @Test
  fun `when normal call is selected and the call permission is denied, directly call the patient with manual dial`() {
    val model = defaultModel()
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))

    spec
        .given(model)
        .whenEvent(NormalCallClicked(permission = Just(DENIED)))
        .then(assertThatNext(
            hasNoModel(),
            hasEffects(DirectCallWithManualDialer(patientPhoneNumber) as ContactPatientEffect)
        ))
  }

  @Test
  fun `when secure call is selected and the call permission is granted, masked call the patient with auto dial`() {
    val model = defaultModel(phoneMaskFeatureEnabled = true)
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))

    spec
        .given(model)
        .whenEvent(SecureCallClicked(permission = Just(GRANTED)))
        .then(assertThatNext(
            hasNoModel(),
            hasEffects(MaskedCallWithAutomaticDialer(patientPhoneNumber = patientPhoneNumber, proxyPhoneNumber = proxyPhoneNumberForSecureCalls) as ContactPatientEffect)
        ))
  }

  @Test
  fun `when secure call is selected and the call permission is denied, masked call the patient with manual dial`() {
    val model = defaultModel(phoneMaskFeatureEnabled = true)
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))

    spec
        .given(model)
        .whenEvent(SecureCallClicked(permission = Just(DENIED)))
        .then(assertThatNext(
            hasNoModel(),
            hasEffects(MaskedCallWithManualDialer(patientPhoneNumber = patientPhoneNumber, proxyPhoneNumber = proxyPhoneNumberForSecureCalls) as ContactPatientEffect)
        ))
  }

  @Test
  fun `when the patient has been marked as agreed to visit, close the screen`() {
    val model = defaultModel(phoneMaskFeatureEnabled = true)
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))

    spec
        .given(model)
        .whenEvent(PatientMarkedAsAgreedToVisit)
        .then(assertThatNext(
            hasNoModel(),
            hasEffects(CloseScreen as ContactPatientEffect)
        ))
  }

  @Test
  fun `when patient agreed to visit is clicked, the patient details must be updated`() {
    val model = defaultModel()
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))

    spec
        .given(model)
        .whenEvent(PatientAgreedToVisitClicked)
        .then(assertThatNext(
            hasNoModel(),
            hasEffects(MarkPatientAsAgreedToVisit(overdueAppointment.appointment.uuid) as ContactPatientEffect)
        ))
  }

  @Test
  fun `when clicking on the next reminder date, the next appointment date from the list of potential dates must be selected as the current selected date`() {
    val remindAppointmentsIn = listOf(
        Days(1),
        Weeks(1)
    )

    val model = defaultModel(remindAppointmentsIn = remindAppointmentsIn)
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))

    val expectedReminderDate = PotentialAppointmentDate(
        scheduledFor = LocalDate.parse("2018-01-08"),
        timeToAppointment = Weeks(1)
    )
    spec
        .given(model)
        .whenEvent(NextReminderDateClicked)
        .then(assertThatNext(
            hasModel(model.reminderDateSelected(expectedReminderDate)),
            hasNoEffects()
        ))
  }

  @Test
  fun `when clicking on the next reminder date, the selected date must not be changed if it is already the latest date available`() {
    val remindAppointmentsIn = listOf(
        Days(1),
        Weeks(1)
    )
    val currentReminderDate = PotentialAppointmentDate(
        scheduledFor = LocalDate.parse("2018-01-08"),
        timeToAppointment = Weeks(1)
    )

    val model = defaultModel(remindAppointmentsIn = remindAppointmentsIn)
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))
        .reminderDateSelected(currentReminderDate)

    spec
        .given(model)
        .whenEvent(NextReminderDateClicked)
        .then(assertThatNext(
            hasNoModel(),
            hasNoEffects()
        ))
  }

  @Test
  fun `when clicking on the previous reminder date, the previous appointment date from the list of potential dates must be selected as the current selected date`() {
    val remindAppointmentsIn = listOf(
        Days(1),
        Weeks(1)
    )
    val currentReminderDate = PotentialAppointmentDate(
        scheduledFor = LocalDate.parse("2018-01-08"),
        timeToAppointment = Weeks(1)
    )

    val model = defaultModel(remindAppointmentsIn = remindAppointmentsIn)
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))
        .reminderDateSelected(currentReminderDate)

    val expectedReminderDate = PotentialAppointmentDate(
        scheduledFor = LocalDate.parse("2018-01-02"),
        timeToAppointment = Days(1)
    )
    spec
        .given(model)
        .whenEvent(PreviousReminderDateClicked)
        .then(assertThatNext(
            hasModel(model.reminderDateSelected(expectedReminderDate)),
            hasNoEffects()
        ))
  }

  @Test
  fun `when clicking on the previous reminder date, the selected date must not be changed if it is already the earliest date available`() {
    val remindAppointmentsIn = listOf(
        Days(1),
        Weeks(1)
    )
    val currentReminderDate = PotentialAppointmentDate(
        scheduledFor = LocalDate.parse("2018-01-02"),
        timeToAppointment = Days(1)
    )

    val model = defaultModel(remindAppointmentsIn = remindAppointmentsIn)
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))
        .reminderDateSelected(currentReminderDate)

    spec
        .given(model)
        .whenEvent(PreviousReminderDateClicked)
        .then(assertThatNext(
            hasNoModel(),
            hasNoEffects()
        ))
  }

  @Test
  fun `when a reminder date is manually selected, the selected date must be set to an existing potential date if it matches`() {
    val model = defaultModel()
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))

    val date = LocalDate.parse("2018-01-08")
    spec
        .given(model)
        .whenEvent(ManualDateSelected(date, LocalDate.now(clock)))
        .then(assertThatNext(
            hasModel(model.reminderDateSelected(PotentialAppointmentDate(scheduledFor = date, timeToAppointment = Weeks(1)))),
            hasNoEffects()
        ))
  }

  @Test
  fun `when a reminder date is manually selected, the selected date must be set to a default date if there is no match`() {
    val model = defaultModel()
        .patientProfileLoaded(patientProfile)
        .overdueAppointmentLoaded(Just(overdueAppointment))

    val date = LocalDate.parse("2018-01-06")
    spec
        .given(model)
        .whenEvent(ManualDateSelected(date, LocalDate.now(clock)))
        .then(assertThatNext(
            hasModel(model.reminderDateSelected(PotentialAppointmentDate(scheduledFor = date, timeToAppointment = Days(5)))),
            hasNoEffects()
        ))
  }

  private fun defaultModel(
      phoneMaskFeatureEnabled: Boolean = false,
      proxyPhoneNumber: String = proxyPhoneNumberForSecureCalls,
      remindAppointmentsIn: List<TimeToAppointment> = this.timeToAppointments
  ): ContactPatientModel {
    val phoneNumberMaskerConfig = PhoneNumberMaskerConfig(proxyPhoneNumber, phoneMaskFeatureEnabled)
    val appointmentConfig = AppointmentConfig(
        appointmentDuePeriodForDefaulters = Period.ZERO,
        scheduleAppointmentsIn = emptyList(),
        defaultTimeToAppointment = Days(0),
        periodForIncludingOverdueAppointments = Period.ZERO,
        remindAppointmentsIn = remindAppointmentsIn
    )

    return ContactPatientModel.create(patientUuid, phoneNumberMaskerConfig, appointmentConfig, clock)
  }
}
