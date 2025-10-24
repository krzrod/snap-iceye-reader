@if exist %userprofile%\.snap\ (
	@rmdir %userprofile%\.snap /s /q
)
@if exist .\target\.snap\ (
	@rmdir .\target\.snap /s /q
)
