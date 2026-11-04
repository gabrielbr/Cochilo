#!/usr/bin/env python3
"""Cochilo's design canvas: every screen and the main flows, for the owner to annotate (docs/design-canvas.md).

    python3 design/canvas/build_canvas.py <folder>   # writes <folder>/project/canvas.json and the artboards

The canvas is a claude.ai Design artifact (URL in docs/design-canvas.md). Each screen is the app's own
Paparazzi screenshot, uploaded to the artifact as an asset; `B` below maps a screen's key to that
upload's /_blob/ url, its pixel size and its caption. A screenshot that changed is uploaded again and
its url replaced here. Flows are artboards of screens joined by arrows labelled with the tap.
"""
import json, os, datetime
import sys
# Where to write: a scratch folder you then publish with the Artifact tool (docs/design-canvas.md).
ROOT = sys.argv[1] if len(sys.argv) > 1 else "build/design-canvas"
P = os.path.join(ROOT, "project"); os.makedirs(P, exist_ok=True)
B = {  # screen key -> (blob url, natural w, natural h, caption)
 "today": ("/_blob/4caae3324529861142e1700a471e407d",461,1000,"Today · awake"),
 "sleeping": ("/_blob/53bcc6eea3ed3a052f9487c66c5f935f",461,1000,"Today · napping (dark)"),
 "night_sleeping": ("/_blob/c409c71d48847afb31585924a64a2c5b",461,1000,"Today · asleep for the night (night light)"),
 "night_waking": ("/_blob/4933e60d4946cc29a2fb5115493cccd2",461,1000,"Today · awake in the night (pt-BR)"),
 "short_nap": ("/_blob/2bc176dec7933780243b0a34df12c52f",461,1000,"Today · after a short nap"),
 "daylight": ("/_blob/d35368b73d5e95bd326f8c347f315166",461,1000,"Today · daylight nudge"),
 "fabric": ("/_blob/5378caf1ef8c79279facfe47ba79e9a3",461,1000,"Today · wrapped in a fabric"),
 "empty": ("/_blob/92ce4f340aeb4b83d91068a4856ee228",461,1000,"Today · empty, first day"),
 "today_full": ("/_blob/c01c42cdb5418f11239222688de4e80b",245,1000,"Today · whole page, Ask Cochilo at the end"),
 "day": ("/_blob/93d68c9ac51b89d8f628bbea27da4412",461,1000,"Day · ring, yesterday"),
 "day_expected": ("/_blob/5548c5bf0285f54c3ae0ba493314009e",461,1000,"Day · today, projected naps on the ring"),
 "day_block": ("/_blob/da1a61800dc5d1e9896d34664da2a85e",461,1000,"Day · expected nap on the timeline"),
 "day_drag": ("/_blob/5131e0488e4983ef8a2f58c4858ed640",461,1000,"Day · dragging a sleep's end"),
 "day_start": ("/_blob/dc3dd6c1080e27e593521e3ae930cc23",461,1000,"Day · dragging a start"),
 "day_pressure": ("/_blob/379f28997b8e841d34c5333e25c891ef",245,1000,"Day · sleep pressure, today (tall)"),
 "tile_awake": ("/_blob/89bd552ae283938e3139e8bfe3a315c1",384,384,"Watch tile · awake"),
 "tile_asleep": ("/_blob/144a3795b812a2889b5081f944ec242a",384,384,"Watch tile · asleep"),
 "tile_feeding": ("/_blob/d4e88697cac6b25b3d4cc2202d938a81",384,384,"Watch tile · feeding"),
 "tile_no_phone": ("/_blob/1cbc322081ba89f12c7c3298e4b145f7",384,384,"Watch tile · no phone"),
 "day_all": ("/_blob/1df545a89d35535ee14714df618979c0",245,1000,"Day · whole page, two tracks"),
 "day_feed": ("/_blob/81b1909521b5eb9bea6d5c6ff19ee508",245,1000,"Day · filtered to Feed"),
 "patterns": ("/_blob/dd93c069714821f92fa2e563ff95edc4",461,1000,"Patterns"),
 "patterns_changes": ("/_blob/1ae62f3e3c01c27b2c3e55e6f745e58a",461,1000,"Patterns · what changed"),
 "patterns_places": ("/_blob/009b127ccd55bb9416742ad1fd44ed5f",461,1000,"Patterns · where naps happened (pt-BR)"),
 "tata": ("/_blob/c676f2e82b684dcaa90af689492365ce",461,1000,"From Tatá"),
 "tata_week": ("/_blob/ce5cc2b383f362170957686cdfaf415d",461,1000,"From Tatá · this week's tip"),
 "age_note": ("/_blob/8f99383f32b32a9660171c54c73553e4",461,1000,"A note by age"),
 "tata_note": ("/_blob/b4b440c6efccdbc505c906f247fd3471",461,1000,"A suggestion from the log"),
 "log_open": ("/_blob/3986f06e1646498e5e6583813def8f53",461,1000,"Log button · open (pt-BR dark)"),
 "log_feed": ("/_blob/2fc0fc966a1238842b2467feb09bd1e3",461,1000,"Log button · Feed › Right"),
 "details_where": ("/_blob/784cfed91cd254ca0ea02245084e0425",461,1000,"Details after · where did she sleep?"),
 "details_pumped": ("/_blob/4d8f95b98038096a226df7d4d74c1d94",461,1000,"Details after · how much (pt-BR dark)"),
 "ticks": ("/_blob/a75b378506d9ea655997adc60a2b43de",461,1000,"T.I.C.K.S. sheet"),
 "ritual": ("/_blob/987f295dd5e8d7b7b860e04571578220",461,1000,"Routine checklist (pt-BR)"),
 "ask": ("/_blob/62cc8bb16384594ccc27b992bc1d0c58",461,1000,"Ask Cochilo"),
 "ask_answer": ("/_blob/cc8157358967da053f4c40512498628e",461,1000,"Ask · an answer (pt-BR)"),
 "ask_not_sure": ("/_blob/dbfae0005aab506236421572c4d6196c",461,1000,"Ask · not sure"),
 "ask_model": ("/_blob/7dd3b71675294eb23970c90d5a4abf07",461,1000,"Ask · the on-phone model's answer"),
 "chats": ("/_blob/ee99b28739afaafe9f5d07309832f85e",461,1000,"Chats (pt-BR)"),
 "model_sheet": ("/_blob/144d1e03c5669f2c5d95a6db4f9912e7",461,1000,"Get the model"),
 "settings": ("/_blob/97335d6cbc617f83158566e368294dd0",461,1000,"Settings"),
 "sync": ("/_blob/3d97019f00adfbb034fd65fbd303584f",461,1000,"Settings › Sync, three states"),
 "initial": ("/_blob/ddd99e2cf4685a7524bd6a500634bf98",461,1000,"Settings › Sync › Your initial"),
 "logged_by": ("/_blob/814c80b222aecd8e4c111a892f8daf03",461,1000,"Who logged it: \"by A\""),
 "reminders": ("/_blob/1e2fd145e3cddc25a88a5145a6e0e3ac",461,1000,"Settings › Notifications (pt-BR)"),
 "fabric_picker": ("/_blob/8581c99f58083d84c0adb4ee1e701426",461,1000,"Settings › Fabric (pt-BR)"),
 "passphrase": ("/_blob/6f824dc1e93c8731d5151cc4cf3179b4",461,1000,"Locked backup · passphrase"),
 "unlock": ("/_blob/453fb47e3e5baa557f9d5fc3d2f13ebc",461,1000,"Locked backup · wrong passphrase (pt-BR dark)"),
 "about": ("/_blob/f93bd73f3e1a30f713abc05d10bfaf5b",461,1000,"About · check for updates"),
 "update": ("/_blob/bb4d2eaab54fb1daa6da54e102d6b525",461,1000,"Update available"),
 "update_dl": ("/_blob/c59efa026adc279e7bbd4a6331db3991",461,1000,"Update · downloading (pt-BR dark)"),
 "pair_looking": ("/_blob/04425eea3a5ff108a880e5e72a9dac82",461,1000,"Pair · looking on the Wi-Fi"),
 "pair_compare": ("/_blob/deed7c631bc799fbd09eedd17fa32fac",461,1000,"Pair · compare the number"),
 "pair_waiting": ("/_blob/e068a7272b8d0f934cfbe2d5280c052f",461,1000,"Pair · waiting for the partner"),
 "pair_done": ("/_blob/58d659916b0768b80b88bffcc673741c",461,1000,"Pair · done"),
 "meet": ("/_blob/fcb8d8f8f0b7b938fa5b0324c1dac842",461,1000,"First run · meet the baby"),
 "look": ("/_blob/25ae18856736af6798400e74c8620c6e",461,1000,"First run · how the baby looks (pt-BR dark)"),
 "notification": ("/_blob/c55235d311a98f9ff1f69ab5a15a0135",461,1000,"Live notification"),
 "widget_awake": ("/_blob/71bc103ae1b1ca655d8f356073794752",461,1000,"Widget · awake"),
 "widget_asleep": ("/_blob/15d052d81870ec397da55a965cd6b01e",461,1000,"Widget · asleep (dark)"),
 "report": ("/_blob/88bac775d2402583b5e37337cdb6f1cb",461,1000,"Report for the doctor (PDF)"),
 "celebration": ("/_blob/ade6235335cadf083ebeee889ea8cec8",461,1000,"Celebration"),
 "tablet": ("/_blob/4dbdb2c5aa0dbaa8e4f1dee442f4b971",625,1000,"Tablet · Today beside Day"),
 "foldable": ("/_blob/bc55dd01e78e50b7618c2263c5a20dca",1000,833,"Foldable · open"),
}
# Each screen's source: app/src/test/snapshots/images/br.gabriel.cochilo.ui_<value>.png
SNAP = {
 "today": "ScreensSnapshotTest_today_light",
 "sleeping": "ScreensSnapshotTest_today_sleeping_dark",
 "night_sleeping": "ScreensSnapshotTest_today_night_light_sleeping",
 "night_waking": "ScreensSnapshotTest_today_night_waking_ptBR_light",
 "short_nap": "ScreensSnapshotTest_today_after_a_short_nap_light",
 "daylight": "ScreensSnapshotTest_today_daylight_nudge_light",
 "fabric": "ScreensSnapshotTest_today_fabric_balao_light",
 "empty": "ScreensSnapshotTest_empty_today_light",
 "today_full": "ScreensSnapshotTest_today_full_light",
 "day": "ScreensSnapshotTest_day_light",
 "day_expected": "ScreensSnapshotTest_day_today_expected_light",
 "day_block": "ScreensSnapshotTest_day_expected_block_light",
 "day_drag": "ScreensSnapshotTest_day_dragging_a_sleep_light",
 "day_start": "ScreensSnapshotTest_day_dragging_a_start_light",
 "day_pressure": "ScreensSnapshotTest_day_pressure_light",
 # The watch tile's screenshots are the wear module's: wear/src/test/snapshots/images/br.gabriel.cochilo.wear_TileSnapshotTest_<name>.png
 "tile_awake": "wear:tile_awake",
 "tile_asleep": "wear:tile_asleep",
 "tile_feeding": "wear:tile_feeding",
 "tile_no_phone": "wear:tile_no_phone",
 "day_all": "ScreensSnapshotTest_day_filter_all_light",
 "day_feed": "ScreensSnapshotTest_day_filter_feed_light",
 "patterns": "ScreensSnapshotTest_patterns_light",
 "patterns_changes": "ScreensSnapshotTest_patterns_changes_light",
 "patterns_places": "ScreensSnapshotTest_patterns_places_ptBR_light",
 "tata": "ScreensSnapshotTest_tata_tab_light",
 "tata_week": "ScreensSnapshotTest_tata_tab_this_week_light",
 "age_note": "ScreensSnapshotTest_age_note_3_4_months_light",
 "tata_note": "ScreensSnapshotTest_tata_note_arms_naps_light",
 "log_open": "ScreensSnapshotTest_log_button_open_ptBR_dark",
 "log_feed": "ScreensSnapshotTest_log_button_feed_right_light",
 "details_where": "ScreensSnapshotTest_details_where_light",
 "details_pumped": "ScreensSnapshotTest_details_pumped_ptBR_dark",
 "ticks": "ScreensSnapshotTest_ticks_sheet_light",
 "ritual": "AvatarSnapshotTest_ritual_checklist_ptBR_light",
 "ask": "ScreensSnapshotTest_ask_light",
 "ask_answer": "ScreensSnapshotTest_ask_nap_length_ptBR_light",
 "ask_not_sure": "ScreensSnapshotTest_ask_not_sure_light",
 "ask_model": "ScreensSnapshotTest_ask_generated_light",
 "chats": "ScreensSnapshotTest_chats_ptBR_light",
 "model_sheet": "ScreensSnapshotTest_model_sheet_app_download_light",
 "settings": "ScreensSnapshotTest_settings_light",
 "sync": "ScreensSnapshotTest_sync_settings_light",
 "initial": "ScreensSnapshotTest_initial_sheet_light",
 "logged_by": "ScreensSnapshotTest_entries_logged_by_partner_light",
 "reminders": "ScreensSnapshotTest_reminders_settings_ptBR_light",
 "fabric_picker": "ScreensSnapshotTest_fabric_picker_ptBR_light",
 "passphrase": "ScreensSnapshotTest_passphrase_sheet_light",
 "unlock": "ScreensSnapshotTest_unlock_sheet_wrong_ptBR_dark",
 "about": "ScreensSnapshotTest_about_updates_light",
 "update": "ScreensSnapshotTest_update_sheet_light",
 "update_dl": "ScreensSnapshotTest_update_sheet_downloading_ptBR_dark",
 "pair_looking": "ScreensSnapshotTest_pair_looking_light",
 "pair_compare": "ScreensSnapshotTest_pair_compare_light",
 "pair_waiting": "ScreensSnapshotTest_pair_waiting_light",
 "pair_done": "ScreensSnapshotTest_pair_done_light",
 "meet": "AvatarSnapshotTest_onboarding_meet_light",
 "look": "AvatarSnapshotTest_onboarding_look_ptBR_dark",
 "notification": "NotificationSnapshotTest_notification_light",
 "widget_awake": "WidgetSnapshotTest_widget_awake_last_left_light",
 "widget_asleep": "WidgetSnapshotTest_widget_asleep_dark",
 "report": "ReportSnapshotTest_report_page_light",
 "celebration": "AvatarSnapshotTest_celebration_full_nap_light",
 "tablet": "ScreensSnapshotTest_today_tablet_light",
 "foldable": "ScreensSnapshotTest_today_foldable_light",
}
assert set(SNAP) == set(B), "every screen needs its snapshot file"

INK="#2a2430"; MUTED="#6b6470"; LINE="#d9d2cc"; PAGE="#faf6f2"; ARROW="#8a8378"; ACCENT="#3d8f86"
HEAD = lambda title: f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>{title}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Libre+Franklin:wght@400;600;700&amp;display=swap" rel="stylesheet">
<style>
body{{margin:0}}
a{{color:{ACCENT}}}a:hover{{color:#2c6b64}}
</style>
</helmet>
'''
def TAIL(w,h): return f'''</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{w},"height":{h}}}}}'>
class Component extends DCLogic {{
renderVals() {{ return {{}}; }}
}}
</script>
</body>
</html>
'''
mid=[0]
def arrow_h(x,y,n):
    mid[0]+=1; i=mid[0]
    return f'<svg width="{n}" height="8" viewBox="0 0 {n} 8" preserveAspectRatio="none" style="position: absolute; left: {x}px; top: {y-4}px; width: {n}px; height: 8px; overflow: visible; fill: none; stroke: {ARROW}; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round"><defs><marker id="dc-arrow-head-filled-{i}" orient="auto" markerWidth="5" markerHeight="5" refX="2" refY="2" overflow="visible"><path d="M0 0 L4 2 L0 4 Z" fill="{ARROW}" stroke="none" style="stroke: none; fill: {ARROW}; fill: context-stroke"/></marker></defs><path d="M 0 4 L {n-4} 4" marker-end="url(#dc-arrow-head-filled-{i})"></path></svg>\n'
def arrow_v(x,y,n):
    mid[0]+=1; i=mid[0]
    return f'<svg width="8" height="{n}" viewBox="0 0 8 {n}" preserveAspectRatio="none" style="position: absolute; left: {x-4}px; top: {y}px; width: 8px; height: {n}px; overflow: visible; fill: none; stroke: {ARROW}; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round"><defs><marker id="dc-arrow-head-filled-{i}" orient="auto" markerWidth="5" markerHeight="5" refX="2" refY="2" overflow="visible"><path d="M0 0 L4 2 L0 4 Z" fill="{ARROW}" stroke="none" style="stroke: none; fill: {ARROW}; fill: context-stroke"/></marker></defs><path d="M 4 0 L 4 {n-4}" marker-end="url(#dc-arrow-head-filled-{i})"></path></svg>\n'
FONT="font-family: 'Libre Franklin', system-ui, sans-serif"
CW, CH, GAP = 230, 499, 170
def card(key,x,y):
    u,w,h,cap = B[key]
    # A watch screen is square: shown whole, centred, on the watch's own dark.
    fit = "object-fit: contain; object-position: center" if w == h else "object-fit: cover; object-position: top"
    bg = "#1c1b1f" if w == h else "#ffffff"
    return f'<img src="{u}" alt="{cap}" style="position: absolute; left: {x}px; top: {y}px; width: {CW}px; height: {CH}px; {fit}; box-sizing: border-box; border: 1px solid {LINE}; border-radius: 12px; background: {bg}">\n'
def cap(text,x,y,w=CW):
    return f'<div style="position: absolute; left: {x}px; top: {y}px; width: {w}px; font-size: 14px; line-height: 18px; font-weight: 600; color: {INK}">{text}</div>\n'
def label(text,x,y,w=150):
    return f'<div style="position: absolute; left: {x}px; top: {y}px; width: {w}px; text-align: center; font-size: 13px; line-height: 16px; color: {MUTED}">{text}</div>\n'
def flow(fname, title, sub, steps, taps):
    """steps: keys in a row; taps: labels between them (len-1)."""
    mid[0]=0
    n=len(steps); W=128+n*CW+(n-1)*GAP; top=168; cy=top+CH//2; H=top+CH+64
    arrows=""; cards=""; labels=""
    for i,k in enumerate(steps):
        x=64+i*(CW+GAP)
        cards+=card(k,x,top)
        labels+=cap(B[k][3],x,top-28)
        if i<n-1:
            ax=x+CW; nlen=(x+CW+GAP-1)-ax
            arrows+=arrow_h(ax,cy,nlen)
            lines=taps[i].count("<br>")+1
            labels+=label(taps[i], ax+GAP//2-75, cy-28-(lines-1)*16)
    body=f'<div style="position: relative; width: {W}px; height: {H}px; background: {PAGE}; color: {INK}; {FONT}">\n'
    body+=arrows+cards
    body+=f'<div style="position: absolute; left: 64px; top: 52px; font-size: 28px; line-height: 34px; font-weight: 700">{title}</div>\n'
    body+=f'<div style="position: absolute; left: 64px; top: 90px; width: {W-128}px; font-size: 15px; line-height: 20px; color: {MUTED}">{sub}</div>\n'
    body+=labels+'</div>\n'
    open(os.path.join(P,fname),"w").write(HEAD(title)+body+TAIL(W,H))
    return W,H
def main_map():
    mid[0]=0
    fname="Main.dc.html"; title="Cochilo: the app map"
    tabs=["today","day","patterns","tata"]; tl=["Day tab","Patterns tab","From Tatá tab"]
    W=128+4*CW+3*GAP; top=168; cy=top+CH//2
    arrows=""; cards=""; labels=""
    for i,k in enumerate(tabs):
        x=64+i*(CW+GAP); cards+=card(k,x,top); labels+=cap(B[k][3],x,top-28)
        if i<3:
            ax=x+CW; arrows+=arrow_h(ax,cy,GAP-1); labels+=label(tl[i],ax+GAP//2-75,cy-28)
    top2=top+CH+150
    for i,(k,lab) in enumerate([("ask","chat icon in the header,<br>every tab"),("settings","gear icon in the header,<br>every tab")]):
        x=64+i*(CW+GAP); cx=x+CW//2
        arrows+=arrow_v(cx,top+CH,top2-1-(top+CH))
        labels+=f'<div style="position: absolute; left: {cx+12}px; top: {top+CH+ (top2-top-CH)//2-16}px; width: 160px; font-size: 13px; line-height: 16px; color: {MUTED}">{lab}</div>\n'
        cards+=card(k,x,top2); labels+=cap(B[k][3],x,top2+CH+12)
    H=top2+CH+12+18+64
    body=f'<div style="position: relative; width: {W}px; height: {H}px; background: {PAGE}; color: {INK}; {FONT}">\n'+arrows+cards
    body+=f'<div style="position: absolute; left: 64px; top: 52px; font-size: 28px; line-height: 34px; font-weight: 700">{title}</div>\n'
    body+=f'<div style="position: absolute; left: 64px; top: 90px; width: {W-128}px; font-size: 15px; line-height: 20px; color: {MUTED}">Four tabs at the bottom; Ask Cochilo and Settings from the header on every tab; the Log button (+) in the corner of every tab. Each screen is the app as it renders (its Paparazzi screenshot).</div>\n'
    body+=labels+'</div>\n'
    open(os.path.join(P,fname),"w").write(HEAD(title)+body+TAIL(W,H)); return W,H
def section(fname,title,keys):
    H_IMG=846; items=""; W=128
    for i,k in enumerate(keys):
        u,w,h,c=B[k]
        # A watch screen is square: about a phone's width, on the watch's own dark.
        watch = w == h
        iw = 390 if watch else round(H_IMG*w/h); ih = 390 if watch else H_IMG; W+=iw+(48 if i else 0)
        bg = "#1c1b1f" if watch else "#ffffff"
        items+=f'<div style="display: flex; flex-direction: column; gap: 12px; width: {iw}px; flex-shrink: 0"><div style="font-size: 14px; line-height: 18px; font-weight: 600; color: {INK}; min-height: 36px">{c}</div><img src="{u}" alt="{c}" style="width: {iw}px; height: {ih}px; box-sizing: border-box; border: 1px solid {LINE}; border-radius: 12px; background: {bg}"></div>\n'
    H=64+48+H_IMG+64
    body=f'<div style="width: {W}px; height: {H}px; box-sizing: border-box; padding: 64px; display: flex; gap: 48px; align-items: flex-start; background: {PAGE}; color: {INK}; {FONT}">\n'+items+'</div>\n'
    open(os.path.join(P,fname),"w").write(HEAD(title)+body+TAIL(W,H)); return W,H

boards={}; order=[]
def add(fname,title,page,x,y,wh):
    boards[fname]={"x":x,"y":y,"w":wh[0],"h":wh[1],"title":title,"page":page}; order.append(fname)
y=0
wh=main_map(); add("Main.dc.html","App map","flows",0,y,wh); y+=wh[1]+120
FLOWS=[
 ("FlowNap.dc.html","A nap","One tap to start, one to end; where she slept is asked after, once.",["today","sleeping","details_where","short_nap"],["Start sleep<br>1 tap","Woke up<br>1 tap","Pick a place<br>1 tap"]),
 ("FlowNight.dc.html","The night","A night waking keeps the night together; Up for the day ends it.",["night_sleeping","night_waking","today"],["Woke up<br>1 tap","Up for the day<br>1 tap (or Back to sleep)"]),
 ("FlowLog.dc.html","Log anything","Press, drag to a kind, slide to a choice, let go: one move. Undo in the snackbar.",["today","log_open","log_feed"],["Hold the + button","Drag to Feed › Right,<br>let go"]),
 ("FlowDay.dc.html","The day, and editing","The Day tab's ring and two tracks; a chip filters one kind; tap a row (pencil) to edit or delete, with Undo.",["day","day_all","day_feed"],["Scroll to Hour by hour","Tap the Feed chip"]),
 ("FlowAsk.dc.html","Ask Cochilo","From the header's chat icon or Today's glimpse; answers from the log, on the phone.",["today","ask","ask_answer","chats"],["Chat icon<br>1 tap","Tap a question<br>1 tap","Chats row"]),
 ("FlowPair.dc.html","Pair a phone","Once, on both phones on the same Wi-Fi; then every change goes across.",["sync","pair_looking","pair_compare","pair_waiting","pair_done"],["Pair a phone","Tap the other phone","Same number","The partner<br>confirms too"]),
 ("FlowFirstRun.dc.html","First run","The baby's name and birth date, then how the baby looks.",["meet","look","empty"],["Continue","Done"]),
 ("FlowUpdate.dc.html","Updates (family build)","Checked on return to the app; the sheet shows the notes; Android confirms.",["about","update","update_dl"],["Check for updates","Update"]),
]
for f in FLOWS:
    wh=flow(f[0],f[1],f[2],f[3],f[4]); add(f[0],f[1],"flows",0,y,wh); y+=wh[1]+120
SECTIONS=[
 ("ScreensToday.dc.html","Today",["today","sleeping","night_sleeping","night_waking","short_nap","daylight","fabric","empty","today_full"]),
 ("ScreensDay.dc.html","Day",["day","day_expected","day_pressure","day_block","day_drag","day_start","day_all","day_feed"]),
 ("ScreensPatternsTata.dc.html","Patterns and From Tatá",["patterns","patterns_changes","patterns_places","tata","tata_week","age_note","tata_note"]),
 ("ScreensLog.dc.html","Logging and sheets",["log_open","log_feed","details_where","details_pumped","ticks","ritual"]),
 ("ScreensAsk.dc.html","Ask Cochilo",["ask","ask_answer","ask_not_sure","ask_model","chats","model_sheet"]),
 ("ScreensSettings.dc.html","Settings",["settings","sync","reminders","fabric_picker","passphrase","unlock","about","update","update_dl"]),
 ("ScreensPair.dc.html","Pairing and who logged it",["pair_looking","pair_compare","pair_waiting","pair_done","initial","logged_by"]),
 ("ScreensOutside.dc.html","First run, the watch and outside the app",["meet","look","notification","widget_awake","widget_asleep","tile_awake","tile_asleep","tile_feeding","tile_no_phone","report","celebration"]),
 ("ScreensBig.dc.html","Tablet and foldable",["tablet","foldable"]),
]
y=0
for f in SECTIONS:
    wh=section(f[0],f[1],f[2]); add(f[0],f[1],"screens",0,y,wh); y+=wh[1]+120
canvas={"v":3,"createdOnFiles":{"v":1,"at":datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")},
 "title":"Cochilo screens and flows","launch":{"view":"canvas","page":"flows"},
 "pages":[{"id":"flows","name":"Flows"},{"id":"screens","name":"Screens"}],
 "boards":boards,"order":order,"notes":{},"designSystems":[]}
json.dump(canvas,open(os.path.join(P,"canvas.json"),"w"),ensure_ascii=False,indent=1)
print(json.dumps({k:(v["w"],v["h"]) for k,v in boards.items()}))
