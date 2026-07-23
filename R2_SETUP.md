# Cloudflare R2 Setup

The app uploads tour images to Cloudflare R2. Secrets are read from environment variables.

1. Copy `.env.example` to `.env`.
2. Replace `TOUR_S3_ACCESS_KEY` and `TOUR_S3_SECRET_KEY` with the R2 credentials shared by the bucket owner.
3. In PowerShell, load the env file:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -and -not $_.StartsWith("#")) {
        $name, $value = $_ -split "=", 2
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}
```

4. Run the app:

```powershell
.\mvnw.cmd spring-boot:run
```

Do not commit `.env` or real R2 secrets to GitHub.
