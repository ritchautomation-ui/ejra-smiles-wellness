/* Public website: sends the appointment request to the Java backend (POST /api/leads). */
const form = document.getElementById("bookForm");
const msg = document.getElementById("msg");
const btn = document.getElementById("submitBtn");

// Do not allow dates in the past
const dateInput = document.getElementById("date");
dateInput.min = new Date().toISOString().split("T")[0];

function show(text, ok) {
  msg.textContent = text;
  msg.className = "msg " + (ok ? "ok" : "err");
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  const val = (id) => document.getElementById(id).value.trim();
  const data = {
    name: val("name"), email: val("email"), phone: val("phone"),
    service: val("service"), preferredDate: val("date"), preferredTime: val("time"),
    notes: val("notes"), source: "Website Form", status: "New"
  };

  // Friendly checks before calling the server (the server validates again)
  if (!data.name || !data.email || !data.phone || !data.service || !data.preferredDate || !data.preferredTime) {
    return show("Please fill in your name, email, mobile number, service, date and time.", false);
  }

  btn.disabled = true;
  show("Sending your request...", true);
  try {
    const res = await fetch(CONFIG.API_BASE + "/leads", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    const json = await res.json();
    if (!res.ok || !json.success) throw new Error(json.error || "Something went wrong.");
    show("Thank you, " + data.name.split(" ")[0] + ". We received your request (reference #" + json.lead.leadId + ") and will contact you soon.", true);
    form.reset();
  } catch (err) {
    if (err instanceof TypeError) show("We could not reach the clinic server. Please try again in a moment.", false);
    else show(err.message, false);
  } finally {
    btn.disabled = false;
  }
});
