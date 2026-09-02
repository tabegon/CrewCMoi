(() => {
    "use strict";

    const REFRESH_MS = 10000;

    const statusBar = document.getElementById("status-bar");

    async function fetchJson(path) {
        const res = await fetch(path, { cache: "no-store" });
        if (!res.ok) {
            throw new Error(path + " -> HTTP " + res.status);
        }
        return res.json();
    }

    function setStatusOk(text) {
        statusBar.textContent = text;
        statusBar.className = "status-bar ok";
    }

    function setStatusError(text) {
        statusBar.textContent = text;
        statusBar.className = "status-bar error";
    }

    function fmtMoney(n) {
        if (typeof n !== "number") return n;
        return n.toLocaleString("fr-FR", { maximumFractionDigits: 2 });
    }

    // --- État du serveur ------------------------------------------------

    async function refreshStatus() {
        const data = await fetchJson("/api/status");
        document.getElementById("stat-online").textContent = data.onlineCount + " / " + data.maxPlayers;
        document.getElementById("stat-max").textContent = data.maxPlayers;
        document.getElementById("stat-version").textContent = data.serverVersion || "-";
        document.getElementById("stat-plugin-version").textContent = data.pluginVersion || "-";
        document.getElementById("stat-tps").textContent = data.tps1m !== undefined ? data.tps1m : "n/a";

        const namesContainer = document.getElementById("stat-online-names");
        namesContainer.innerHTML = "";
        (data.onlinePlayers || []).forEach(name => {
            const chip = document.createElement("span");
            chip.className = "chip";
            chip.textContent = name;
            namesContainer.appendChild(chip);
        });
        if ((data.onlinePlayers || []).length === 0) {
            namesContainer.innerHTML = '<span class="chip">Aucun joueur en ligne</span>';
        }
    }

    // --- Config.yml (arbre générique) -----------------------------------

    function renderConfigNode(value) {
        if (value !== null && typeof value === "object" && !Array.isArray(value)) {
            const wrapper = document.createElement("div");
            wrapper.className = "cfg-section";
            for (const [key, val] of Object.entries(value)) {
                const row = document.createElement("div");
                row.className = "cfg-row";
                if (val !== null && typeof val === "object") {
                    const label = document.createElement("div");
                    label.innerHTML = '<span class="cfg-key">' + escapeHtml(key) + "</span>";
                    row.appendChild(label);
                    row.appendChild(renderConfigNode(val));
                } else {
                    row.innerHTML = '<span class="cfg-key">' + escapeHtml(key) + '</span>: '
                        + '<span class="cfg-value">' + escapeHtml(String(val)) + "</span>";
                }
                wrapper.appendChild(row);
            }
            return wrapper;
        }
        const span = document.createElement("span");
        span.className = "cfg-value";
        span.textContent = String(value);
        return span;
    }

    async function refreshConfig() {
        const data = await fetchJson("/api/config");
        const container = document.getElementById("config-tree");
        container.innerHTML = "";
        container.appendChild(renderConfigNode(data));
    }

    // --- Baltop -----------------------------------------------------------

    async function refreshBaltop() {
        const data = await fetchJson("/api/economy/top?limit=50");
        const tbody = document.querySelector("#table-baltop tbody");
        tbody.innerHTML = "";
        data.forEach(row => {
            const tr = document.createElement("tr");
            const rankClass = row.rank <= 3 ? ' class="rank-' + row.rank + '"' : "";
            tr.innerHTML = "<td" + rankClass + ">#" + row.rank + "</td>"
                + "<td>" + escapeHtml(row.name) + "</td>"
                + "<td>" + fmtMoney(row.balance) + "</td>";
            tbody.appendChild(tr);
        });
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3">Aucune donnée</td></tr>';
        }
    }

    // --- Primes -------------------------------------------------------

    async function refreshBounties() {
        const data = await fetchJson("/api/bounties");
        const tbody = document.querySelector("#table-bounties tbody");
        tbody.innerHTML = "";
        data.forEach(row => {
            const tr = document.createElement("tr");
            tr.innerHTML = "<td>" + escapeHtml(row.target) + "</td>"
                + "<td>" + fmtMoney(row.totalAmount) + "</td>"
                + "<td>" + row.contributorCount + "</td>";
            tbody.appendChild(tr);
        });
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3">Aucune prime active</td></tr>';
        }
    }

    // --- Prix de vente ----------------------------------------------------

    let allPrices = [];

    async function refreshPrices() {
        allPrices = await fetchJson("/api/economy/prices");
        renderPrices(allPrices);
    }

    function renderPrices(list) {
        const tbody = document.querySelector("#table-prices tbody");
        tbody.innerHTML = "";
        list.forEach(row => {
            const tr = document.createElement("tr");
            tr.innerHTML = "<td>" + escapeHtml(row.material) + "</td><td>" + fmtMoney(row.price) + "</td>";
            tbody.appendChild(tr);
        });
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="2">Aucun résultat</td></tr>';
        }
    }

    // --- Claims -------------------------------------------------------

    let allClaims = [];

    async function refreshClaims() {
        allClaims = await fetchJson("/api/claims");
        renderClaims(allClaims);
    }

    function renderClaims(list) {
        const tbody = document.querySelector("#table-claims tbody");
        tbody.innerHTML = "";
        list.forEach(claim => {
            const tr = document.createElement("tr");
            const flagsHtml = Object.entries(claim.flags || {}).map(([flag, perm]) => {
                const cls = perm.toLowerCase();
                return '<span class="tag ' + cls + '" title="' + escapeHtml(flag) + '">' + escapeHtml(flag) + "</span>";
            }).join(" ");
            tr.innerHTML = "<td>" + escapeHtml(claim.owner) + "</td>"
                + "<td>" + escapeHtml(claim.world) + "</td>"
                + "<td>" + claim.chunkX + " / " + claim.chunkZ + "</td>"
                + "<td>" + claim.trustedCount + "</td>"
                + "<td>" + (claim.forSale ? fmtMoney(claim.salePrice) : "-") + "</td>"
                + "<td>" + flagsHtml + "</td>";
            tbody.appendChild(tr);
        });
        if (list.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6">Aucun claim</td></tr>';
        }
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    // --- Filtres locaux ----------------------------------------------------

    document.getElementById("prices-search").addEventListener("input", (e) => {
        const q = e.target.value.trim().toLowerCase();
        renderPrices(allPrices.filter(p => p.material.toLowerCase().includes(q)));
    });

    document.getElementById("claims-search").addEventListener("input", (e) => {
        const q = e.target.value.trim().toLowerCase();
        renderClaims(allClaims.filter(c =>
            (c.owner || "").toLowerCase().includes(q) || (c.world || "").toLowerCase().includes(q)));
    });

    // --- Boucle de rafraîchissement ----------------------------------------

    async function refreshAll() {
        try {
            await Promise.all([
                refreshStatus(),
                refreshConfig(),
                refreshBaltop(),
                refreshBounties(),
                refreshPrices(),
                refreshClaims()
            ]);
            setStatusOk("Connecté — mis à jour à " + new Date().toLocaleTimeString("fr-FR"));
        } catch (err) {
            console.error(err);
            setStatusError("Erreur de connexion au plugin : " + err.message);
        }
    }

    refreshAll();
    setInterval(refreshAll, REFRESH_MS);
})();
