import re

# Read the file
with open('backend/src/test/java/com/docassistant/document/service/DocumentServicePropertyTest.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Add SystemLogService mock after DocumentPreviewService
content = re.sub(
    r'(DocumentPreviewService documentPreviewService = mock\(DocumentPreviewService\.class\);)',
    r'\1\n        com.docassistant.auth.service.SystemLogService systemLogService = mock(com.docassistant.auth.service.SystemLogService.class);',
    content
)

# Update DocumentServiceImpl constructor calls
content = re.sub(
    r'new DocumentServiceImpl\(\s*documentRepository,\s*fileStorageService,\s*storageQuotaService,\s*documentPreviewService\)',
    'new DocumentServiceImpl(\n            documentRepository, fileStorageService, storageQuotaService, documentPreviewService, systemLogService)',
    content
)

# Write the file back
with open('backend/src/test/java/com/docassistant/document/service/DocumentServicePropertyTest.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("File fixed successfully")
