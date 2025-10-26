@if exist %userprofile%\.snap\ (
	@rmdir %userprofile%\.snap /s /q
)
@if exist .\target\.snap\ (
	@rmdir .\target\.snap /s /q
)

@xcopy /s /y /q .\s1tbx  .\target\nbm\clusters\s1tbx > nul