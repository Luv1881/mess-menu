package com.example.myapplication.repository

import android.content.Context
import android.net.Uri
import com.example.myapplication.model.DayMenu
import com.example.myapplication.model.WeeklyMenu
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream

/**
 * Parser for Excel files containing weekly mess menu data.
 * Supports both .xlsx (XSSF) and .xls (HSSF) formats.
 * 
 * Expected Excel format:
 * | Day       | Breakfast    | Lunch        | Dinner       |
 * |-----------|--------------|--------------|--------------|
 * | Monday    | Bread, Eggs  | Rice, Dal    | Chapati, Veg |
 * | Tuesday   | ...          | ...          | ...          |
 * | ...       | ...          | ...          | ...          |
 */
class ExcelParser(private val context: Context) {
    
    sealed class ParseResult {
        data class Success(val menu: WeeklyMenu) : ParseResult()
        data class Error(val message: String, val exception: Exception? = null) : ParseResult()
    }
    
    /**
     * Parse an Excel file from the given URI.
     * @param uri URI of the Excel file
     * @return ParseResult containing either the parsed menu or an error
     */
    fun parseExcel(uri: Uri): ParseResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ParseResult.Error("Could not open file")
            
            val fileName = getFileName(uri)
            val isXlsx = fileName?.endsWith(".xlsx", ignoreCase = true) ?: true
            
            inputStream.use { stream ->
                val workbook = createWorkbook(stream, isXlsx)
                workbook.use { wb ->
                    parseWorkbook(wb, uri.toString())
                }
            }
        } catch (e: Exception) {
            ParseResult.Error("Failed to parse Excel file: ${e.message}", e)
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun createWorkbook(inputStream: InputStream, isXlsx: Boolean): Workbook {
        return if (isXlsx) {
            XSSFWorkbook(inputStream)
        } else {
            HSSFWorkbook(inputStream)
        }
    }
    
    private fun parseWorkbook(workbook: Workbook, sourcePath: String): ParseResult {
        if (workbook.numberOfSheets == 0) {
            return ParseResult.Error("Excel file has no sheets")
        }
        
        val sheet = workbook.getSheetAt(0)
        val dayMenus = mutableListOf<DayMenu>()
        
        val headerInfo = detectHeaders(sheet)
            ?: return ParseResult.Error("Could not find valid headers. Expected: Day, Breakfast, Lunch, Dinner")
        
        for (rowIndex in (headerInfo.headerRowIndex + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            
            val dayMenu = parseRow(row, headerInfo)
            if (dayMenu != null && dayMenu.dayOfWeek.isNotBlank()) {
                dayMenus.add(dayMenu)
            }
            
            if (dayMenus.size >= 7) break
        }
        
        if (dayMenus.isEmpty()) {
            return ParseResult.Error("No menu data found in the Excel file")
        }
        
        val completeDays = DayMenu.DAYS_OF_WEEK.map { dayName ->
            dayMenus.find { it.dayOfWeek.equals(dayName, ignoreCase = true) }
                ?: DayMenu.empty(dayName)
        }
        
        return ParseResult.Success(
            WeeklyMenu(
                days = completeDays,
                sourcePath = sourcePath,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
    
    private data class HeaderInfo(
        val headerRowIndex: Int,
        val dayColumn: Int,
        val breakfastColumn: Int,
        val lunchColumn: Int,
        val dinnerColumn: Int
    )
    
    private fun detectHeaders(sheet: Sheet): HeaderInfo? {
        for (rowIndex in 0..minOf(10, sheet.lastRowNum)) {
            val row = sheet.getRow(rowIndex) ?: continue
            
            var dayCol = -1
            var breakfastCol = -1
            var lunchCol = -1
            var dinnerCol = -1
            
            for (cellIndex in 0..row.lastCellNum) {
                val cell = row.getCell(cellIndex) ?: continue
                val value = getCellStringValue(cell).lowercase().trim()
                
                when {
                    value.contains("day") || value == "days" -> dayCol = cellIndex
                    value.contains("breakfast") || value == "morning" -> breakfastCol = cellIndex
                    value.contains("lunch") || value == "afternoon" -> lunchCol = cellIndex
                    value.contains("dinner") || value == "evening" || value == "night" -> dinnerCol = cellIndex
                }
            }
            
            if (dayCol >= 0 && breakfastCol >= 0 && lunchCol >= 0 && dinnerCol >= 0) {
                return HeaderInfo(rowIndex, dayCol, breakfastCol, lunchCol, dinnerCol)
            }
        }
        
        // Try default column positions if no headers found
        // Assume: Col 0 = Day, Col 1 = Breakfast, Col 2 = Lunch, Col 3 = Dinner
        val firstDataRow = sheet.getRow(0) ?: return null
        if (firstDataRow.lastCellNum >= 3) {
            return HeaderInfo(-1, 0, 1, 2, 3)
        }
        
        return null
    }
    
    private fun parseRow(row: Row, headerInfo: HeaderInfo): DayMenu? {
        val day = getCellStringValue(row.getCell(headerInfo.dayColumn)).trim()
        
        val normalizedDay = normalizeDay(day)
        if (normalizedDay == null) return null
        
        val breakfast = getCellStringValue(row.getCell(headerInfo.breakfastColumn)).trim()
        val lunch = getCellStringValue(row.getCell(headerInfo.lunchColumn)).trim()
        val dinner = getCellStringValue(row.getCell(headerInfo.dinnerColumn)).trim()
        
        return DayMenu(
            dayOfWeek = normalizedDay,
            breakfast = breakfast,
            lunch = lunch,
            dinner = dinner
        )
    }
    
    private fun normalizeDay(day: String): String? {
        val lowerDay = day.lowercase()
        return DayMenu.DAYS_OF_WEEK.find { it.lowercase().startsWith(lowerDay.take(3)) }
    }
    
    private fun getCellStringValue(cell: Cell?): String {
        if (cell == null) return ""
        
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> cell.numericCellValue.toString()
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        cell.stringCellValue
                    } catch (e: Exception) {
                        cell.numericCellValue.toString()
                    }
                }
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
