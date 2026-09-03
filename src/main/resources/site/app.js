(() => {
    "use strict";

    const REFRESH_MS = 10000;
    const MC_VERSION = "1.21.1";
    const statusBar = document.getElementById("status-bar");
    
    let cache = {
        baltop: [],
        players: [],
        bounties: [],
        claims: [],
        shop: { listings: [], lastSale: null }
    };

    const TEXTURE_FIXES = {
        "golden_apple": ["item/golden_apple", "item/apple_golden"],
        "enchanted_golden_apple": ["item/enchanted_golden_apple", "item/golden_apple"],
        "bee_nest": ["block/bee_nest_front", "block/bee_nest_side", "block/bee_nest_bottom"],
        "beehive": ["block/beehive_front", "block/beehive_side"],
        "tipped_arrow": ["item/tipped_arrow", "item/tipped_arrow_head", "item/arrow"],
        "arrow": ["item/arrow"],
        "spectral_arrow": ["item/spectral_arrow"],
        "clock": ["item/clock_00", "item/clock"],
        "compass": ["item/compass_00", "item/compass"],
        "crossbow": ["item/crossbow"],
        "recovery_compass": ["item/recovery_compass_00", "item/recovery_compass"]
    };

    const loginScreen = document.getElementById("login-screen");
    const appScreen = document.getElementById("app");

    document.getElementById("login-form").addEventListener("submit", (e) => {
        e.preventDefault();
        
        const user = document.getElementById("login-user").value;
        const pass = document.getElementById("login-pass").value;

        if (user === "admin" && pass === "titou") {
            loginScreen.classList.add("hidden");
            appScreen.classList.remove("hidden");
            refreshAll();
            setInterval(refreshAll, REFRESH_MS);
        } else {
            alert("Nom d'utilisateur ou mot de passe incorrect.");
        }
    });

    document.querySelectorAll(".tab-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
            document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));
            
            btn.classList.add("active");
            document.getElementById(btn.dataset.tab).classList.add("active");
        });
    });

    async function fetchJson(path, defaultVal = []) {
        try {
            const res = await fetch(path, { cache: "no-store" });
            if (!res.ok) {
                console.warn(`Avertissement API: ${path} a renvoyé un statut HTTP ${res.status}`);
                return defaultVal; 
            }
            return await res.json();
        } catch (err) {
            console.error(`Erreur réseau sur ${path}:`, err);
            return defaultVal;
        }
    }

    function fmtMoney(n) {
        return (typeof n === "number") ? n.toLocaleString("fr-FR", { maximumFractionDigits: 2 }) + " $" : "0 $";
    }

    function escapeHtml(str) {
        return String(str || '').replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }

    async function refreshStatus() {
        const data = await fetchJson("/api/status");
        
        document.getElementById("stat-online").textContent = data.onlineCount || 0;
        document.getElementById("stat-max").textContent = data.maxPlayers || 0;
        document.getElementById("stat-version").textContent = data.serverVersion || "-";
        document.getElementById("stat-tps").textContent = data.tps1m !== undefined ? data.tps1m : "20.0";

        const stateEl = document.getElementById("stat-state");
        const status = (data.status || (data.onlineCount !== undefined ? "ONLINE" : "OFFLINE")).toUpperCase();
        if (status === "ONLINE") stateEl.innerHTML = '<span class="badge online">En Ligne</span>';
        else if (status === "RESTARTING") stateEl.innerHTML = '<span class="badge restarting">En Redémarrage</span>';
        else stateEl.innerHTML = '<span class="badge offline">Hors Ligne</span>';
      
        const namesContainer = document.getElementById("stat-online-names");
        namesContainer.innerHTML = "";
        const onlineList = data.onlinePlayers || [];
        
        if (onlineList.length === 0) {
            namesContainer.innerHTML = '<span class="chip">Aucun joueur connecté</span>';
        } else {
            onlineList.forEach(name => {
                const chip = document.createElement("span");
                chip.className = "chip";
                chip.textContent = name;
                namesContainer.appendChild(chip);
            });
        }
    }

    async function refreshBaltop() {
        cache.baltop = await fetchJson("/api/economy/top?limit=500");
        
        const totalMoney = cache.baltop.reduce((sum, row) => sum + (row.balance || 0), 0);
        document.getElementById("stat-total-money").textContent = fmtMoney(totalMoney);

        renderBaltop(cache.baltop);
    }

    function renderBaltop(list) {
        const tbody = document.querySelector("#table-baltop tbody");
        tbody.innerHTML = "";
        list.forEach(row => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td>#${row.rank}</td>
                            <td><strong>${escapeHtml(row.name)}</strong></td>
                            <td class="text-gold">${fmtMoney(row.balance)}</td>`;
            tbody.appendChild(tr);
        });
        if (list.length === 0) tbody.innerHTML = '<tr><td colspan="3">Aucun résultat</td></tr>';
    }

    document.getElementById("bank-search").addEventListener("input", (e) => {
        const q = e.target.value.toLowerCase();
        renderBaltop(cache.baltop.filter(p => p.name.toLowerCase().includes(q)));
    });

    async function refreshPlayers() {
        cache.players = await fetchJson("/api/players");
        renderPlayers(cache.players);
    }

    function renderPlayers(list) {
        const onlineGrid = document.getElementById("players-online-grid");
        const offlineGrid = document.getElementById("players-offline-grid");
        onlineGrid.innerHTML = "";
        offlineGrid.innerHTML = "";

        list.forEach(p => {
            const card = document.createElement("div");
            card.className = "player-card";
            card.innerHTML = `
                <img src="https://mc-heads.net/avatar/${p.name}/32" class="player-avatar" alt="${p.name}">
                <div>
                    <div><strong>${escapeHtml(p.name)}</strong></div>
                    <small class="text-gold">${fmtMoney(p.balance)}</small>
                </div>
            `;
            card.addEventListener("click", () => openPlayerModal(p));

            if (p.online) onlineGrid.appendChild(card);
            else offlineGrid.appendChild(card);
        });
    }

    document.getElementById("players-search").addEventListener("input", (e) => {
        const q = e.target.value.toLowerCase();
        renderPlayers(cache.players.filter(p => p.name.toLowerCase().includes(q)));
    });

    const modal = document.getElementById("player-modal");
    document.getElementById("modal-close").addEventListener("click", () => modal.classList.add("hidden"));

    function openPlayerModal(p) {
        document.getElementById("modal-player-name").textContent = p.name;
        document.getElementById("modal-avatar").src = `https://mc-heads.net/avatar/${p.name}/48`;
        
        const statusEl = document.getElementById("modal-player-status");
        statusEl.className = p.online ? "badge online" : "badge offline";
        statusEl.textContent = p.online ? "En Ligne" : "Hors Ligne";

        document.getElementById("modal-last-seen").textContent = p.lastLogin || "Inconnu";
        document.getElementById("modal-playtime").textContent = p.playTime || "n/a";

        const worldEl = document.getElementById("modal-world");
        const coordsEl = document.getElementById("modal-coords");

        if (p.lastCoords) {
            if (worldEl) worldEl.textContent = p.lastCoords.world || "Inconnu";
            if (coordsEl) coordsEl.textContent = `X: ${p.lastCoords.x} | Y: ${p.lastCoords.y} | Z: ${p.lastCoords.z}`;
        } else {
            if (worldEl) worldEl.textContent = p.online ? "Inconnu" : "Hors-ligne";
            if (coordsEl) coordsEl.textContent = "Inconnues";
        }

        const balEl = document.getElementById("modal-balance");
        balEl.textContent = fmtMoney(p.balance);
        balEl.className = "text-gold";

        const cmdList = document.getElementById("modal-commands");
        cmdList.innerHTML = (p.lastCommands || []).slice(-3).map(c => `<li>${escapeHtml(c)}</li>`).join("") || "<li>Aucune commande</li>";

        const msgList = document.getElementById("modal-messages");
        msgList.innerHTML = (p.lastMessages || []).slice(-5).map(m => `<li>${escapeHtml(m)}</li>`).join("") || "<li>Aucun message</li>";

        const invGrid = document.getElementById("modal-inventory");
        invGrid.innerHTML = "";
        const invSlots = (p.inventory && p.inventory.length > 0) ? p.inventory : new Array(36).fill(null);

        invSlots.forEach(item => {
            const slot = document.createElement("div");
            slot.className = "inv-slot";

            if (item && item.type) {
                const rawName = item.type.toLowerCase();
                slot.title = `${item.type} (x${item.amount})`;

                const img = document.createElement("img");
                img.className = "inv-icon";

                let possiblePaths = [];
                if (TEXTURE_FIXES[rawName]) {
                    possiblePaths = [...TEXTURE_FIXES[rawName]];
                } else {
                    possiblePaths = [`item/${rawName}`, `block/${rawName}`];
                }

                let currentPathIdx = 0;
                const tryNextTexture = () => {
                    if (currentPathIdx < possiblePaths.length) {
                        const path = possiblePaths[currentPathIdx++];
                        img.src = `https://assets.mcasset.cloud/${MC_VERSION}/assets/minecraft/textures/${path}.png`;
                    } else {
                        img.style.display = "none";
                    }
                };

                img.onerror = tryNextTexture;
                tryNextTexture();

                slot.appendChild(img);

                if (item.amount > 1) {
                    const countBadge = document.createElement("span");
                    countBadge.className = "inv-amount";
                    countBadge.textContent = item.amount;
                    slot.appendChild(countBadge);
                }
            }

            invGrid.appendChild(slot);
        });

        modal.classList.remove("hidden");
    }

    async function refreshBounties() {
        cache.bounties = await fetchJson("/api/bounties");
        renderBounties(cache.bounties);
    }

    function renderBounties(list) {
        const tbody = document.querySelector("#table-bounties tbody");
        tbody.innerHTML = "";
        list.forEach(row => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td><strong>${escapeHtml(row.target)}</strong></td>
                            <td class="text-gold">${fmtMoney(row.totalAmount)}</td>
                            <td>${row.contributorCount}</td>`;
            tbody.appendChild(tr);
        });
        if (list.length === 0) tbody.innerHTML = '<tr><td colspan="3">Aucune prime active</td></tr>';
    }

    document.getElementById("bounty-search").addEventListener("input", (e) => {
        const q = e.target.value.toLowerCase();
        renderBounties(cache.bounties.filter(b => b.target.toLowerCase().includes(q)));
    });

    async function refreshClaims() {
        cache.claims = await fetchJson("/api/claims");
        renderClaims(cache.claims);
    }

    function renderClaims(list) {
        const tbody = document.querySelector("#table-claims tbody");
        tbody.innerHTML = "";
        list.forEach(c => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td><code>${escapeHtml(c.id || "N/A")}</code></td>
                            <td>${escapeHtml(c.owner)}</td>
                            <td>${escapeHtml(c.world)}</td>
                            <td>X: ${c.chunkX * 16} / Z: ${c.chunkZ * 16}</td>`;
            tbody.appendChild(tr);
        });
        if (list.length === 0) tbody.innerHTML = '<tr><td colspan="4">Aucun claim trouvé</td></tr>';
    }

    document.getElementById("claims-search").addEventListener("input", (e) => {
        const q = e.target.value.toLowerCase();
        renderClaims(cache.claims.filter(c => 
            (c.owner || "").toLowerCase().includes(q) || (c.id || "").toString().includes(q)
        ));
    });

async function refreshShop() {
        try {
            // Modification ici : on tape sur /api/ah comme défini dans ton Java
            const data = await fetchJson("/api/ah", { listings: [], lastSale: null });
            cache.shop = data; // On met bien à jour le cache
            renderShop(cache.shop);
        } catch (e) {
            console.error("Erreur chargement shop", e);
        }
    }

    function renderShop(data) {
        const lastSaleContainer = document.getElementById("last-sale-container");
        if (lastSaleContainer) {
            if (data.lastSale) {
                lastSaleContainer.innerHTML = `
                    <p><strong>Objet :</strong> <span class="text-gold">${escapeHtml(data.lastSale.item || "Inconnu")}</span> (x${data.lastSale.amount || 1})</p>
                    <p><strong>Prix de vente :</strong> <span class="text-gold">${fmtMoney(data.lastSale.price)}</span></p>
                    <p><strong>Acheteur :</strong> ${escapeHtml(data.lastSale.buyer || "Inconnu")}</p>
                    <p><strong>Vendeur :</strong> ${escapeHtml(data.lastSale.seller || "Inconnu")}</p>
                `;
            } else {
                lastSaleContainer.innerHTML = `<p>Aucune vente récente enregistrée.</p>`;
            }
        }

        renderShopListings(data.listings || []);
    }

    function renderShopListings(listings) {
        const tbody = document.querySelector("#table-shop tbody");
        if (!tbody) return;
        
        tbody.innerHTML = "";

        listings.forEach(row => {
            const tr = document.createElement("tr");
            const itemName = row.item ? row.item.type : "Inconnu";
            const itemAmount = row.item ? row.item.amount : 1;
            const seller = row.sellerName || "Inconnu";

            tr.innerHTML = `
                <td><strong>${escapeHtml(itemName)}</strong></td>
                <td>${itemAmount}</td>
                <td class="text-gold">${fmtMoney(row.price)}</td>
                <td>${escapeHtml(seller)}</td>
            `;
            tbody.appendChild(tr);
        });

        if (listings.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4">Aucun objet en vente actuellement.</td></tr>';
        }
    }

    document.getElementById("shop-search").addEventListener("input", (e) => {
        const q = e.target.value.toLowerCase();
        const listings = cache.shop.listings || [];
        const filtered = listings.filter(s => {
            const itemName = (s.item && s.item.type) ? s.item.type.toLowerCase() : "";
            const sellerName = (s.sellerName) ? s.sellerName.toLowerCase() : "";
            return itemName.includes(q) || sellerName.includes(q);
        });
        renderShopListings(filtered);
    });
  
    async function refreshAll() {
        try {
            await Promise.all([
                refreshStatus(),
                refreshBaltop(),
                refreshPlayers(),
                refreshBounties(),
                refreshClaims(),
                refreshShop()
            ]);
            statusBar.textContent = "Connecté — " + new Date().toLocaleTimeString("fr-FR");
            statusBar.className = "status-bar ok";
        } catch (err) {
            console.error(err);
            statusBar.textContent = "Erreur de synchro API";
            statusBar.className = "status-bar error";
        }
    }
})();