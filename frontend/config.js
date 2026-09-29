/*
 * config.js  -  the ONLY place where the backend address is written.
 * Both the public website and the admin dashboard use CONFIG.API_BASE.
 *
 * >>> AFTER DEPLOYING TO RAILWAY, replace YOUR-RAILWAY-DOMAIN below <<<
 *     Example: "https://ejra-smiles-production.up.railway.app/api"
 *     (no trailing slash; keep "/api" at the end)
 */
const RAILWAY_API = "https://YOUR-RAILWAY-DOMAIN.up.railway.app/api";   // <-- EDIT THIS

const CONFIG = {
  API_BASE:
    (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1")
      ? "http://localhost:8080/api"      // local development
      : RAILWAY_API                       // deployed on Vercel
};
