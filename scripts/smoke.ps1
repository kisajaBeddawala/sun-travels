param([string]$Api = 'http://127.0.0.1:8080/api')

$ErrorActionPreference = 'Stop'

function Post-Json([string]$Path, [hashtable]$Body) {
    Invoke-RestMethod -Method Post -Uri "$Api/$Path" -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 8)
}

function Search-Contract([long]$ContractId, [string]$Date, [int]$Nights, [int[]]$Adults) {
    $response = Post-Json 'search' @{
        checkInDate = $Date
        numberOfNights = $Nights
        rooms = @($Adults | ForEach-Object { @{ adults = $_ } })
    }
    $response.results | Where-Object { $_.contractId -eq $ContractId }
}

$hotel = Post-Json 'hotels' @{
    name = 'Smoke Test ' + [guid]::NewGuid().ToString('N').Substring(0, 8)
    city = 'Colombo'
}
$contract = Post-Json 'contracts' @{
    hotelId = $hotel.id
    startDate = '2027-01-01'
    endDate = '2027-12-31'
    markupPercentage = '15.00'
    currency = 'USD'
    roomTypes = @(
        @{ name = 'Standard'; ratePerAdultPerNight = '35.00'; roomCount = 15; maxAdults = 2 }
        @{ name = 'Sea View'; ratePerAdultPerNight = '55.00'; roomCount = 8; maxAdults = 3 }
    )
}

$saved = Invoke-RestMethod -Uri "$Api/contracts/$($contract.id)"
if ($saved.hotelId -ne $hotel.id -or @($saved.roomTypes).Count -ne 2) {
    throw 'The saved agreement did not contain both room types.'
}

$results = @(Search-Contract $contract.id '2027-06-10' 3 @(2, 1))
$standard = $results | Where-Object roomType -EQ 'Standard'
$seaView = $results | Where-Object roomType -EQ 'Sea View'
if ($standard.totalPrice -ne '362.25' -or $seaView.totalPrice -ne '569.25' -or
    $standard.availability -ne 'AVAILABLE' -or $seaView.availability -ne 'AVAILABLE') {
    throw 'The demo quotes or availability did not match the expected results.'
}

$overCapacity = Search-Contract $contract.id '2027-06-10' 3 @(3, 1) | Where-Object roomType -EQ 'Standard'
if ($overCapacity.availability -ne 'NOT_AVAILABLE' -or $null -ne $overCapacity.totalPrice -or
    $overCapacity.reasons -notcontains 'OCCUPANCY_EXCEEDED') {
    throw 'Standard did not report the expected occupancy failure.'
}

if (@(Search-Contract $contract.id '2027-12-31' 1 @(1)).Count -ne 2 -or
    @(Search-Contract $contract.id '2027-12-31' 2 @(1)).Count -ne 0) {
    throw 'The final contracted night was evaluated incorrectly.'
}

Write-Output "Smoke check passed for contract $($contract.id)."
