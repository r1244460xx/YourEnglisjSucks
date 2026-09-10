[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$BaseUrl = "http://localhost:8080"

Write-Host "========================================================="
Write-Host "   YourEnglishSucks End-to-End (E2E) System Verification"
Write-Host "========================================================="

# Step 1: Check Docker PostgreSQL
Write-Host "`n[Step 1] Checking Docker PostgreSQL readiness..."
docker exec yourenglishsucks-postgres pg_isready -U postgres
if ($LASTEXITCODE -ne 0) {
    Write-Error "PostgreSQL container is not ready!"
    exit 1
}
Write-Host "  -> PostgreSQL container is ready!"

# Step 2: Test backend /api/config
Write-Host "`n[Step 2] Testing GET $BaseUrl/api/config..."
$config = Invoke-RestMethod -Uri "$BaseUrl/api/config" -Method Get
Write-Host "  -> Backend status: $($config.status), Model: $($config.model)"

# Step 3: Round 1 Polish
Write-Host "`n[Step 3] Executing Round 1 English Polish (POST /api/conversations/polish)..."
$rawText = "I am write this email for inform you about the project status. Because our team meet some difficult problem in database connection, so the launch date maybe delay for two weeks. Please kindly understand our situation."
$polishBody = @{
    rawText = $rawText
} | ConvertTo-Json

$polishRes = Invoke-RestMethod -Uri "$BaseUrl/api/conversations/polish" -Method Post -Body $polishBody -ContentType "application/json; charset=utf-8"
$convId = $polishRes.conversation.id
$convTitle = $polishRes.conversation.title

Write-Host "  -> Created Conversation ID: $convId"
Write-Host "  -> Auto-generated Title: '$convTitle'"
Write-Host "  -> Message count in response: $($polishRes.messages.Count)"
$aiReply = $polishRes.messages[1].content

# Step 4: Verify Database Persistence in PostgreSQL container
Write-Host "`n[Step 4] Querying PostgreSQL container for dual-table persistence..."
$rawCount = (docker exec yourenglishsucks-postgres psql -U postgres -d yourenglishsucks -t -c "SELECT count(*) FROM polish_raw_submissions WHERE conversation_id = '$convId';").Trim()
$msgCount = (docker exec yourenglishsucks-postgres psql -U postgres -d yourenglishsucks -t -c "SELECT count(*) FROM chat_messages WHERE conversation_id = '$convId';").Trim()

Write-Host "  -> polish_raw_submissions rows: $rawCount (expected: 1)"
Write-Host "  -> chat_messages rows: $msgCount (expected: 2)"

if ($rawCount -ne "1" -or $msgCount -ne "2") {
    Write-Error "Database dual-table persistence failed!"
    exit 1
}

# Step 5: Follow-up question in the same session
Write-Host "`n[Step 5] Testing Follow-up question in same session (POST /api/conversations/$convId/messages)..."
$followUpBody = @{
    message = "Could you make this more casual and friendly for an internal Slack message?"
} | ConvertTo-Json

$followUpRes = Invoke-RestMethod -Uri "$BaseUrl/api/conversations/$convId/messages" -Method Post -Body $followUpBody -ContentType "application/json; charset=utf-8"
Write-Host "  -> Follow-up response received. Round: $($followUpRes.roundNumber), Sender: $($followUpRes.senderType)"

$msgCountAfter = (docker exec yourenglishsucks-postgres psql -U postgres -d yourenglishsucks -t -c "SELECT count(*) FROM chat_messages WHERE conversation_id = '$convId';").Trim()
Write-Host "  -> chat_messages rows after follow-up: $msgCountAfter (expected: 4)"
if ($msgCountAfter -ne "4") {
    Write-Error "Follow-up message persistence failed!"
    exit 1
}

# Step 6: Test manual title renaming (Option C)
Write-Host "`n[Step 6] Testing manual Title Renaming (PATCH /api/conversations/$convId/title)..."
$renameBody = @{
    title = "Database Connection Delay Notice"
} | ConvertTo-Json

$renameRes = Invoke-RestMethod -Uri "$BaseUrl/api/conversations/$convId/title" -Method Patch -Body $renameBody -ContentType "application/json; charset=utf-8"
Write-Host "  -> Renamed title: '$($renameRes.title)'"

$dbTitle = (docker exec yourenglishsucks-postgres psql -U postgres -d yourenglishsucks -t -c "SELECT title FROM conversations WHERE id = '$convId';").Trim()
Write-Host "  -> Title in PostgreSQL container: '$dbTitle'"
if ($dbTitle -ne "Database Connection Delay Notice") {
    Write-Error "Title rename in database failed!"
    exit 1
}

# Step 7: Load all conversations at once
Write-Host "`n[Step 7] Testing loading all conversations (GET /api/conversations)..."
$allConvs = Invoke-RestMethod -Uri "$BaseUrl/api/conversations" -Method Get
Write-Host "  -> Total conversations returned: $($allConvs.Count)"
$matched = $allConvs | Where-Object { $_.id -eq $convId }
if ($matched) {
    Write-Host "  -> Successfully found active conversation: '$($matched.title)'"
} else {
    Write-Error "Conversation not found in list!"
    exit 1
}

# Step 8: Input validations
Write-Host "`n[Step 8] Testing 4000 character limit and blank input validations..."
$tooLong = "A" * 4001
$badBody1 = @{ rawText = $tooLong } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/conversations/polish" -Method Post -Body $badBody1 -ContentType "application/json"
    Write-Error "Expected 400 Bad Request for >4000 chars, but succeeded!"
    exit 1
} catch {
    Write-Host "  -> [PASSED] Rejected >4000 characters with 400 Bad Request"
}

$badBody2 = @{ rawText = "   " } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/conversations/polish" -Method Post -Body $badBody2 -ContentType "application/json"
    Write-Error "Expected 400 Bad Request for blank input, but succeeded!"
    exit 1
} catch {
    Write-Host "  -> [PASSED] Rejected blank input with 400 Bad Request"
}

# Step 9: Delete conversation
Write-Host "`n[Step 9] Testing Delete conversation (DELETE /api/conversations/$convId)..."
Invoke-RestMethod -Uri "$BaseUrl/api/conversations/$convId" -Method Delete
$afterDelete = Invoke-RestMethod -Uri "$BaseUrl/api/conversations" -Method Get
$stillThere = $afterDelete | Where-Object { $_.id -eq $convId }
if (-not $stillThere) {
    Write-Host "  -> [PASSED] Conversation successfully archived/removed from active list"
} else {
    Write-Error "Conversation still exists after delete!"
    exit 1
}

Write-Host "`n========================================================="
Write-Host " SUCCESS: All End-to-End (E2E) Tests Passed 100%!"
Write-Host "========================================================="
