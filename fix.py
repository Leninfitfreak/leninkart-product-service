import os
import hvac
import requests
from kubernetes import client, config

print("\n" + "="*80)
print(" VAULT + KUBERNETES FULL DIAGNOSTIC (READ-ONLY)")
print("="*80)

# -----------------------------------------------------------------------------
# CONFIG (edit only if needed)
# -----------------------------------------------------------------------------
VAULT_ADDR = os.environ.get("VAULT_ADDR", "http://127.0.0.1:8200")
VAULT_TOKEN = os.environ.get("VAULT_TOKEN")  # must be set
K8S_NAMESPACE = "dev"
VAULT_NAMESPACE = "vault"

# -----------------------------------------------------------------------------
# 1. BASIC VAULT CONNECTIVITY
# -----------------------------------------------------------------------------
print("\n[1] VAULT CONNECTIVITY")
print("-"*80)
print(f"VAULT_ADDR = {VAULT_ADDR}")
print(f"VAULT_TOKEN set = {'YES' if VAULT_TOKEN else 'NO'}")

if not VAULT_TOKEN:
    print("❌ VAULT_TOKEN not set. Stop here.")
    exit(1)

vault = hvac.Client(url=VAULT_ADDR, token=VAULT_TOKEN)

try:
    status = vault.sys.read_health_status(method="GET")
    print("Vault Health:", status)
except Exception as e:
    print("❌ Vault not reachable:", e)
    exit(1)

# -----------------------------------------------------------------------------
# 2. AUTH METHODS
# -----------------------------------------------------------------------------
print("\n[2] ENABLED AUTH METHODS")
print("-"*80)
auths = vault.sys.list_auth_methods()
for path, cfg in auths.items():
    print(f"{path} -> type={cfg.get('type')}")

# -----------------------------------------------------------------------------
# 3. KUBERNETES AUTH CONFIG
# -----------------------------------------------------------------------------
print("\n[3] KUBERNETES AUTH CONFIG")
print("-"*80)
try:
    k8s_auth = vault.read("auth/kubernetes/config")
    print(k8s_auth)
except Exception as e:
    print("❌ Cannot read kubernetes auth config:", e)

# -----------------------------------------------------------------------------
# 4. VAULT ROLES
# -----------------------------------------------------------------------------
print("\n[4] VAULT KUBERNETES ROLES")
print("-"*80)
try:
    roles = vault.list("auth/kubernetes/role")
    print("Roles:", roles)
    if roles and "data" in roles:
        for role in roles["data"]["keys"]:
            print(f"\n--- Role: {role} ---")
            print(vault.read(f"auth/kubernetes/role/{role}"))
except Exception as e:
    print("❌ Error reading roles:", e)

# -----------------------------------------------------------------------------
# 5. POLICIES
# -----------------------------------------------------------------------------
print("\n[5] VAULT POLICIES")
print("-"*80)
policies = vault.sys.list_policies()
for p in policies:
    print(p)

# -----------------------------------------------------------------------------
# 6. SECRET ENGINES
# -----------------------------------------------------------------------------
print("\n[6] ENABLED SECRET ENGINES")
print("-"*80)
engines = vault.sys.list_mounted_secrets_engines()
for path, cfg in engines.items():
    print(f"{path} -> type={cfg.get('type')}")

# -----------------------------------------------------------------------------
# 7. TEST READ: LENINKART SECRETS
# -----------------------------------------------------------------------------
print("\n[7] TEST READ: LENINKART SECRETS")
print("-"*80)
paths = [
    "secret/data/leninkart/product-service/database",
    "secret/data/leninkart/order-service/database",
    "secret/data/leninkart/kafka/credentials",
]

for p in paths:
    try:
        res = vault.read(p)
        print(f"\n✔ {p}")
        print(res["data"]["data"] if res else "EMPTY")
    except Exception as e:
        print(f"\n❌ {p} -> {e}")

# -----------------------------------------------------------------------------
# 8. KUBERNETES SIDE (SERVICE ACCOUNTS)
# -----------------------------------------------------------------------------
print("\n[8] KUBERNETES SERVICE ACCOUNTS")
print("-"*80)

try:
    config.load_kube_config()
    v1 = client.CoreV1Api()

    sa_list = v1.list_namespaced_service_account(K8S_NAMESPACE)
    for sa in sa_list.items:
        print(f"SA: {sa.metadata.name}")

except Exception as e:
    print("❌ Kubernetes access failed:", e)

# -----------------------------------------------------------------------------
# 9. EXTERNAL-SECRETS SERVICE ACCOUNT TOKEN CHECK
# -----------------------------------------------------------------------------
print("\n[9] ESO SERVICE ACCOUNT TOKEN CHECK")
print("-"*80)
try:
    sa = v1.read_namespaced_service_account("external-secrets", "external-secrets-system")
    print("external-secrets SA found")
    print("Secrets attached:", sa.secrets)
except Exception as e:
    print("❌ external-secrets SA issue:", e)

# -----------------------------------------------------------------------------
# DONE
# -----------------------------------------------------------------------------
print("\n" + "="*80)
print(" DIAGNOSTIC COMPLETE")
print("="*80)