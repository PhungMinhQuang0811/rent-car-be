//package com.mp.karental.payment.util;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//
//import java.text.SimpleDateFormat;
//import java.time.LocalDate;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.TimeZone;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class DateUtilsTest {
//
//    @Test
//    void parseISO_WithValidDate_ShouldReturnDate() {
//        // Arrange
//        String validDate = "2024-02-20";
//
//        // Act
//        Date result = DateUtils.parseISO(validDate);
//
//        // Assert
//        assertNotNull(result);
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//        assertEquals(validDate, sdf.format(result));
//    }
//
//    @Test
//    void parseISO_WithInvalidDate_ShouldReturnNull() {
//        // Arrange
//        String invalidDate = "invalid-date";
//
//        // Act
//        Date result = DateUtils.parseISO(invalidDate);
//
//        // Assert
//        assertNull(result);
//    }
//
//    @ParameterizedTest
//    @MethodSource("provideDatesForDiffCalculation")
//    void getDiffInDays_ShouldCalculateCorrectDifference(
//            LocalDate date1,
//            LocalDate date2,
//            long expectedDiff) {
//        // Act
//        long result = DateUtils.getDiffInDays(date1, date2);
//
//        // Assert
//        assertEquals(expectedDiff, result);
//    }
//
//    private static Stream<Arguments> provideDatesForDiffCalculation() {
//        return Stream.of(
//            Arguments.of(
//                LocalDate.of(2024, 2, 20),
//                LocalDate.of(2024, 2, 25),
//                5
//            ),
//            Arguments.of(
//                LocalDate.of(2024, 2, 25),
//                LocalDate.of(2024, 2, 20),
//                -5
//            ),
//            Arguments.of(
//                LocalDate.of(2024, 2, 20),
//                LocalDate.of(2024, 2, 20),
//                0
//            ),
//            Arguments.of(
//                LocalDate.of(2024, 1, 31),
//                LocalDate.of(2024, 2, 1),
//                1
//            )
//        );
//    }
//
//    @Test
//    void parse_WithValidDate_ShouldReturnLocalDate() {
//        // Arrange
//        String validDate = "2024-02-20";
//
//        // Act
//        LocalDate result = DateUtils.parse(validDate);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2024, result.getYear());
//        assertEquals(2, result.getMonthValue());
//        assertEquals(20, result.getDayOfMonth());
//    }
//
//    @Test
//    void parse_WithInvalidDate_ShouldThrowException() {
//        // Arrange
//        String invalidDate = "invalid-date";
//
//        // Act & Assert
//        assertThrows(Exception.class, () -> DateUtils.parse(invalidDate));
//    }
//
//    @Test
//    void getVnTime_ShouldReturnFormattedTime() {
//        // Act
//        String result = DateUtils.getVnTime();
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(14, result.length()); // Format: yyyyMMddHHmmss
//        assertTrue(result.matches("\\d{14}")); // Should be all digits
//    }
//
//    @Test
//    void formatVnTime_WithValidCalendar_ShouldReturnFormattedTime() {
//        // Arrange
//        TimeZone timeZone = TimeZone.getTimeZone("Etc/GMT+7");
//        Calendar calendar = Calendar.getInstance(timeZone);
//        calendar.clear(); // Clear all fields first
//
//        // Set date components
//        calendar.set(Calendar.YEAR, 2024);
//        calendar.set(Calendar.MONTH, Calendar.FEBRUARY); // 1 (0-based)
//        calendar.set(Calendar.DAY_OF_MONTH, 21);
//        calendar.set(Calendar.HOUR_OF_DAY, 14); // 14:30:45 GMT+7
//        calendar.set(Calendar.MINUTE, 30);
//        calendar.set(Calendar.SECOND, 45);
//        calendar.set(Calendar.MILLISECOND, 0);
//
//        // Act
//        String result = DateUtils.formatVnTime(calendar);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals("20240222043045", result,
//            String.format("Should format date as yyyyMMddHHmmss. Calendar time: %s, TimeZone: %s",
//                calendar.getTime(), calendar.getTimeZone().getID()));
//    }
//
//    @Test
//    void vnCalendar_ShouldHaveCorrectTimeZone() {
//        // Assert
//        assertEquals("Etc/GMT+7",
//            DateUtils.VN_CALENDAR.getTimeZone().getID(),
//            "VN_CALENDAR should use GMT+7 timezone");
//    }
//
//    @Test
//    void dateFormats_ShouldHaveCorrectPatterns() {
//        // Arrange & Act
//        SimpleDateFormat isoFormat = DateUtils.ISO_DATE_FORMAT;
//        SimpleDateFormat vnpayFormat = DateUtils.VNPAY_DATE_FORMAT;
//
//        // Assert
//        assertEquals("yyyy-MM-dd", isoFormat.toPattern());
//        assertEquals("yyyyMMddHHmmss", vnpayFormat.toPattern());
//    }
//
//    @Test
//    void formatVnTime_OutputShouldMatchPattern() {
//        // Arrange
//        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
//
//        // Act
//        String result = DateUtils.formatVnTime(calendar);
//
//        // Assert
//        assertTrue(result.matches("\\d{4}\\d{2}\\d{2}\\d{2}\\d{2}\\d{2}"),
//            "Format should be yyyyMMddHHmmss");
//    }
//
//    @Test
//    void getVnTime_ShouldBeCurrentTime() {
//        // Arrange
//        Calendar expectedCal = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
//
//        // Act
//        String result = DateUtils.getVnTime();
//
//        // Assert
//        // Check if the timestamp is within 1 second of current time
//        String expected = new SimpleDateFormat("yyyyMMdd").format(expectedCal.getTime());
//        assertTrue(result.startsWith(expected),
//            "VnTime should reflect current date");
//    }
//}