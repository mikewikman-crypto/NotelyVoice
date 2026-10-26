package com.module.notelycompose.export.domain

interface ExportSelectionInteractor {
    fun exportAllSelection(
        texts: List<String>,
        titles: List<String>,
        audioPath: List<String>,
        createdDates: List<String>,
        shouldExportAudio: Boolean,
        shouldExportTxt: Boolean,
        shouldExportMd: Boolean,
        shouldUploadDropbox: Boolean,
        onProgress: (Float) -> Unit,
        onResult: (Result<String>) -> Unit
    )
}
