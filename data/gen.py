#!/usr/bin/env python3
"""Generate verbs.json for Conjugaison FR from Verbiste XML (GPLv2+, Pierre Sarrazin)."""
import xml.etree.ElementTree as ET
import json, datetime, sys

CONJ = ET.parse('conjugations-fr.xml').getroot()
VERBS = ET.parse('verbs-fr.xml').getroot()

def first_i(p):
    """Ending for one person. None => no <i> child => defective (no form).
    "" => <i></i> => valid empty ending (form == radical)."""
    if p is None:
        return None
    i = p.find('i')
    if i is None:
        return None
    return i.text or ""

def plist(node):
    return [first_i(p) for p in node.findall('p')] if node is not None else []

TPL = {}
for t in CONJ.findall('template'):
    TPL[t.attrib['name']] = {
        'inf_end': first_i(t.find('Infinitif/infinitif-présent/p')) or "",
        'pres': plist(t.find('Indicatif/présent')),
        'imp':  plist(t.find('Indicatif/imparfait')),
        'fut':  plist(t.find('Indicatif/futur-simple')),
        'cond': plist(t.find('Conditionnel/présent')),
        'subj': plist(t.find('Subjonctif/présent')),
        'impe': plist(t.find('Imperatif/imperatif-présent')),
        'pp':   plist(t.find('Participe/participe-passé')),
    }

VMAP = {}   # infinitive -> (template_name, english)
for v in VERBS.findall('v'):
    inf = v.find('i').text
    tn = v.find('t').text
    en = v.find('en').text if v.find('en') is not None else ""
    VMAP[inf] = (tn, en or "")

# être-auxiliary verbs (masc default agreement); everything else uses avoir
ETRE = {
    "aller", "venir", "arriver", "partir", "entrer", "sortir", "monter",
    "descendre", "rester", "tomber", "naître", "mourir", "devenir",
    "revenir", "retourner", "rentrer",
}

# Curated practice list: high-frequency French verbs.
VERB_LIST = """
être avoir aller faire dire pouvoir vouloir savoir voir venir devoir prendre
trouver donner falloir parler mettre passer aimer croire demander rester
répondre entrer sembler laisser rappeler porter connaître partir comprendre
sentir attendre rendre arriver vivre chercher sortir tenir montrer devenir
tomber revenir suivre penser regarder appeler permettre écrire recevoir
commencer manger boire lire finir choisir dormir courir ouvrir offrir sourire
rire écouter travailler jouer chanter danser marcher gagner perdre payer
essayer envoyer acheter jeter espérer préférer lever appeler nettoyer employer
oublier étudier habiter aider raconter expliquer occuper décider utiliser
continuer arrêter compter garder monter descendre entrer retourner rentrer
naître mourir grandir réussir réfléchir remplir obéir punir applaudir bâtir
attaquer changer nager voyager ranger partager plonger corriger bouger
avancer lancer effacer placer remplacer prononcer
connaître paraître apparaître disparaître mettre permettre promettre battre
conduire construire produire traduire détruire suffire plaire craindre
peindre atteindre éteindre joindre rejoindre résoudre coudre
apprendre reprendre entreprendre surprendre
tenir obtenir retenir soutenir maintenir contenir appartenir
sortir servir mentir partir dormir sentir
falloir pleuvoir
""".split()

IMPERSONAL = {"falloir", "pleuvoir", "neiger"}

PERSONS = ["je", "tu", "il", "nous", "vous", "ils"]

def conj_simple(radical, endings, impers=False):
    if not endings or len(endings) < 6:
        return None
    out = []
    for idx, e in enumerate(endings):
        if (impers and idx != 2) or e is None:
            out.append(None)          # defective / not applicable
        else:
            out.append(radical + e)   # e may be "" -> form is the bare radical
    return out

def build(inf):
    if inf not in VMAP:
        return None, f"not in Verbiste"
    tn, en = VMAP[inf]
    tpl = TPL.get(tn)
    if not tpl:
        return None, f"no template {tn}"
    ie = tpl['inf_end']
    radical = inf[:-len(ie)] if ie and inf.endswith(ie) else inf
    impers = inf in IMPERSONAL
    pres = conj_simple(radical, tpl['pres'], impers)
    imp = conj_simple(radical, tpl['imp'], impers)
    fut = conj_simple(radical, tpl['fut'], impers)
    cond = conj_simple(radical, tpl['cond'], impers)
    subj = conj_simple(radical, tpl['subj'], impers)
    impe = None
    if not impers:
        impe_raw = tpl['impe']
        if impe_raw and len(impe_raw) == 3 and any(e is not None for e in impe_raw):
            impe = [None if e is None else radical + e for e in impe_raw]
            if all(e is None for e in impe):
                impe = None
    pp = (radical + tpl['pp'][0]) if tpl['pp'] and tpl['pp'][0] is not None else None

    aux = "être" if inf in ETRE else "avoir"
    pc = None
    if pp:
        if aux == "avoir":
            av = ["ai", "as", "a", "avons", "avez", "ont"]
            pc = [f"{a} {pp}" for a in av]
        else:
            ev = ["suis", "es", "est", "sommes", "êtes", "sont"]
            # masc default: sg = pp, pl = pp + 's' (unless already ends in s)
            ppl = pp if pp.endswith("s") else pp + "s"
            forms_pp = [pp, pp, pp, ppl, ppl, ppl]
            pc = [f"{e} {p}" for e, p in zip(ev, forms_pp)]
        if impers:
            pc = [None, None, pc[2], None, None, None]

    grp = 3
    if inf.endswith("er") and inf != "aller":
        grp = 1
    elif inf.endswith("ir") and tn == "fin:ir":
        grp = 2

    t = {}
    if pres: t["pres"] = pres
    if imp: t["imp"] = imp
    if fut: t["fut"] = fut
    if cond: t["cond"] = cond
    if subj: t["subj"] = subj
    if impe: t["impe"] = impe
    if pc: t["pc"] = pc

    rec = {
        "inf": inf, "en": en, "group": grp, "aux": aux,
        "pp": pp, "t": t,
    }
    if impers:
        rec["impers"] = True
    return rec, None

seen = set()
out_verbs = []
problems = []
for inf in VERB_LIST:
    if inf in seen:
        continue
    seen.add(inf)
    d, err = build(inf)
    if err:
        problems.append((inf, err))
    else:
        out_verbs.append(d)

out_verbs.sort(key=lambda v: v["inf"])
data = {
    "meta": {
        "source": "Conjugations derived from Verbiste (Pierre Sarrazin), GPLv2+",
        "generated": datetime.date.today().isoformat(),
        "persons": PERSONS,
        "persons_impe": ["tu", "nous", "vous"],
        "tenses": {"pres": "présent", "imp": "imparfait", "fut": "futur simple",
                   "cond": "conditionnel présent", "subj": "subjonctif présent",
                   "impe": "impératif", "pc": "passé composé"},
    },
    "verbs": out_verbs,
}
json.dump(data, open('verbs.json', 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
print(f"verbs written: {len(out_verbs)}")
if problems:
    print("PROBLEMS:")
    for p in problems:
        print("  ", p)
