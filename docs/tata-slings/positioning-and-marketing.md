# Cochilo, da Tatá Slings: positioning and marketing

> Draft, 2026-11-02, to discuss with Tatá. Pairs with [monetization.md](monetization.md),
> [brand-voice.md](brand-voice.md) and [design-system.md](design-system.md).

## 1. Where Cochilo sits in the brand

| Line | What it does for the parent | Age |
|---|---|---|
| Tatá Swaddle | Calms the newborn: "O swaddle que abraça como colo" | 0–3 months |
| Tatá Fácil Sling | Colo with free hands | birth to 15 kg |
| Bolsinha Calminha, Naninha | "Sono e aconchego" | 0+ |
| TatáT.O / TatáTessori | Play and development, with a therapist's eye | 3 months+ |
| E-book Parentalidade Positiva | Guidance | 0–3 years |
| **Cochilo** | **Understands the baby's sleep, including sleep in the sling and in arms** | **0–24 months** |

Today "Sono e aconchego" sells things that help the baby calm down. Cochilo adds what happens next:
when to offer the next nap, how the day is going, and the reassurance that contact naps count. It
also gives the brand something it doesn't have yet: a reason to stay on the parent's phone every
day after the box is opened.

## 2. Positioning statement

**For** Brazilian mothers and fathers of babies up to 2 years old who carry their babies and want
calmer days without rigid sleep training,
**Cochilo** is the baby-sleep diary from Tatá Slings
**that** tells you when the next nap is likely, shows the reasons behind every guess, counts naps in
the sling and in arms as real sleep, and keeps everything on your phone,
**unlike** Napper, Huckleberry and other foreign apps, which are subscriptions, keep your data in
their cloud, hide how they predict, and treat contact naps as a habit to break.

## 3. The four pillars

| Pillar | Claim (pt-BR) | Proof |
|---|---|---|
| **Colo conta** | "Toda soneca conta. Até no colo." | "Onde dormiu?" log (berço, colo, sling…); contact naps on the timeline and in Patterns in their own colour |
| **Explica o porquê** | "A gente mostra a conta." | Prediction card shows the numbers it used; research in [docs/research.md](../research.md) |
| **Fica no seu celular** | "Seus dados ficam com você. Sem conta, sem nuvem." | The Play build has no INTERNET permission; export/import files only |
| **Feito por quem entende** | "Feito em casa, por uma terapeuta ocupacional e mãe, e pelo pai do Dante." | Tatá's credentials and notes; the family story |

**Tagline (pick one with Tatá):**
1. **"Toda soneca conta. Até no colo."** (recommended: says the brand and the product at once)
2. "O sono do seu bebê, explicado com calma."
3. "Do colo para o sono, com calma."

## 4. Audience

**Primary: the parent who just bought a sling.** First-time mothers, 25–38, Brazil-wide, buying
online, already following Tatá on Instagram, talking to her on WhatsApp. The baby is 0–4 months:
exactly when the sleep rhythm forms and when parents search "janela de sono 2 meses" at 3 a.m.

**Secondary: the second parent and the grandparents.** They do the night shifts and want to know
"when did he last sleep?". Cochilo's backup/import already lets two phones share the log; say so.

**Tertiary: professionals.** Sleep consultants (Tatá is working with one now), doulas, TOs and
babywearing educators, who ask families to keep a sleep diary on paper or in WhatsApp.

**Not for:** parents looking for a cry-it-out programme or a baby monitor. Say so, kindly.

## 5. Competition, in one table

| | Napper | Huckleberry | Generic trackers (BabyCenter, Sprout, Bebê Conecta) | **Cochilo** |
|---|---|---|---|---|
| Language/market | Swedish, translated | US, translated | Mixed | Brazilian, pt-BR first |
| Price | R$ 22,90–114,90 (Unlimited R$ 89,90) | ~€130/year Premium | Freemium + ads | Free; Cochilo+ R$ 39,90 once, or free with a sling |
| Data | Cloud account | Cloud account | Cloud, ads | On the phone only |
| Prediction | Black box | Black box (SweetSpot) | Basic or none | Shows its numbers |
| Contact naps | A log option | A log option | Rarely | A pillar, with sling tips from a TO |
| Person behind it | Company | Company | Company | Tatá |

We don't win on features. We win on trust, language, price and the fact that the parent already
knows the person behind it.

## 6. Launch plan

### Phase 0: ready (November 2026)
- Tatá approves name, tagline, icon, voice, Play copy.
- Privacy page on tataslings.com.br/cochilo-privacidade ("O Cochilo não coleta nada. Ponto.").
- Landing page tataslings.com.br/cochilo (a Nuvemshop content page): what it is, 3 screenshots,
  the Play button, Tatá's video. This is where every QR code points, so we can count scans with UTMs.
- Play listing in pt-BR (primary) and English. Keywords: "sono do bebê", "soneca", "janela de sono",
  "rotina do bebê", "diário do bebê", "amamentação". Screenshots in the Tatá Slings style with one
  line each, in her voice.

### Phase 1: quiet beta (December 2026)
- 30 customers from the WhatsApp list with babies 0–6 months; invite on WhatsApp, in her words:
  "Meu marido fez um app para o sono do Dante, e eu queria que você testasse antes de todo mundo."
- Google Play closed testing (it also covers Play's tester requirement for new developer accounts).
- Ask three questions after two weeks on WhatsApp: did you open it every day, what confused you, what
  would make you recommend it.

### Phase 2: launch (January 2027)
- **The story is the launch.** A Reel/TikTok by Tatá: "O pai do Dante fez um app para o sono dele.
  Agora é de vocês." Real home, Dante in the sling (correct position), the app on the phone.
- A Live with the sleep consultant (if she agrees): "Sono no colo: pode?" with Cochilo as the
  takeaway tool.
- Insert card in every box from this date (QR + code; [monetization §4](monetization.md)).
- Order-confirmation e-mail and the post-purchase WhatsApp message get one line: "Seu sling chegou
  com um presente: o Cochilo+, app de sono do bebê."
- Newsletter to the store list.

### Phase 3: keep it going (from February 2027)
Cadence that fits Tatá's time: **2 posts and 1 Reel a week** about sleep, plus what she already posts.

## 7. Channels

| Channel | Role | What we do | Measure |
|---|---|---|---|
| **The box** | Main acquisition | QR card, code, 1-line in e-mail and WhatsApp | Scans of /cochilo with `utm_source=card`; codes used (asked in the beta) |
| **Instagram / TikTok / YouTube Shorts** (@tataslings) | Awareness, trust | Series below | Reach, saves, link clicks with UTM |
| **SEO on tataslings.com.br** | Free search traffic that also sells slings | "Tudo sobre" style pages answering sleep questions (list below) | Search Console clicks; orders from those pages |
| **Google Play (ASO)** | Organic installs | pt-BR keywords, screenshots, replies to every review in her voice | Installs, rating ≥ 4.6 |
| **Google Ads** (already R$ 90 per conversion) | Conversion booster, not app ads | Add "Ganhe o app Cochilo+" as a sitelink/callout on sling ads; A/B for 4 weeks | Conversion rate and CPA before/after |
| **WhatsApp** | Support, retention | Post-purchase message; a "dica de sono" at 2, 4 and 6 weeks after delivery, only for those who opt in | Replies, repeat orders |
| **Partners** | Credibility, reach | Sleep consultants, doulas, TOs, babywearing educators, baby-shower lists | Cochilo Pro codes issued; orders with partner coupons |

### Content series (Instagram/TikTok)

1. **"Toda soneca conta"**: short Reels about contact naps, exterogestação, sling naps done safely.
2. **"Pergunta da madrugada"**: answering real DMs ("luz vermelha ou amarela?", Tatá's own question;
   the answer is in [research §2.3](../research.md)).
3. **"Os números do Dante"** *(only if Tatá and Gabriel are comfortable sharing)*: a week of
   Cochilo screenshots, what changed (daylight walks, the bedtime ritual).
4. **"Tatá explica"**: one rule, one reason (rosto visível; barriga para cima; parar o swaddle quando
   começar a rolar).
5. **Behind the scenes**: how the app was made at home, the same story as the slings.

### SEO pages (each links to Cochilo and to the matching product)

| Search (pt-BR) | Page | Product link |
|---|---|---|
| janela de sono por idade / bebê de 3 meses | "Janela de sono: o que é e por que cada bebê tem a sua" | Cochilo |
| quantas sonecas o bebê faz por dia | "Sonecas por idade, sem tabela mágica" | Cochilo |
| bebê só dorme no colo | "Bebê que só dorme no colo: é normal?" | Tatá Fácil Sling |
| soneca no sling é segura | "Soneca no sling com segurança" | Tatá Fácil Sling |
| luz vermelha ou amarela para o bebê | "Luz da madrugada: qual cor e quanto" | Cochilo |
| até quando usar swaddle | "Swaddle: quando começar e quando parar" | Tatá Swaddle |
| rotina de sono do bebê | "Ritual de sono em 5 passos" | Cochilo (ritual builder) |

Every page: answer in the first paragraph, Tatá's voice, a safety line where relevant, sources at
the end (the research doc already has them), and the not-medical-advice note.

## 8. Rules for anything we publish

- No medical claims. Never "cura cólica", "faz dormir a noite toda", "previne", "monitora".
  Cochilo "ajuda a entender"; the paediatrician decides.
- Safe-sleep and safe-babywearing images only (design-system §6). AAP 2022 for sleep; her own guide
  for positioning.
- Advertising to parents, never to children (CONAR rules on children's products apply to the
  products, and the app is for adults; the Play listing targets 18+ and stays out of the Families
  programme).
- No fake reviews or "most downloaded" claims. Real testimonials with permission.
- Family privacy: decide with Tatá what of Dante and Francisco appears, and keep to it.

## 9. What success looks like at 6 months *(targets to agree, not forecasts)*

| Metric | Target |
|---|---|
| Installs | 2,000 |
| Share of sling buyers who activate Cochilo+ | 25% |
| Play rating | ≥ 4.6 with ≥ 50 reviews |
| Orders with `utm_source=cochilo` | ≥ 40/month |
| Google Ads CPA on sling campaigns | 10% lower than the R$ 90 baseline |
| Tatá's time on Cochilo | ≤ 3 h/week (if it's more, cut scope) |

## 10. Risks

| Risk | Mitigation |
|---|---|
| Tatá's time is the bottleneck (store, two kids, a new baby) | Content in batches; reuse "Tudo sobre" pages; Gabriel drafts, Tatá approves |
| A bug at 3 a.m. hurts the store's 4.8 Reclame Aqui reputation | Closed beta, "Report a problem" opens WhatsApp, no release without the AGENTS.md checks |
| iPhone users can't install it | Say "por enquanto só Android" plainly; collect interest on the landing page; decide on iOS after 6 months |
| "Cochilo" is a common word; hard to own in search and trademark | Use "Cochilo da Tatá" in titles; check INPI before printing cards |
| Being read as medical advice | Not-medical-advice note in the app and on every page; no promises (§8) |
| Play policies on health apps | Declare it as a parenting diary; no health claims; data safety form: "no data collected" |

## 11. Open questions for Tatá

1. Name: "Cochilo", "Cochilo da Tatá" or a "Tatá…" name like her other lines?
2. Is she comfortable being the face of the app, and of showing Dante's data?
3. Which tagline?
4. Free Cochilo+ with every sling, or only above a ticket (for example the Fácil Sling and the
   Swaddle)?
5. Would she write the guides, or co-write them with the sleep consultant?
6. Teal UI as today's site, or warmer (Pérola page + navy ink, as proposed)?
