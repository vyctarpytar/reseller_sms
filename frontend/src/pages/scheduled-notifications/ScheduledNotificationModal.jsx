import React, { useEffect, useMemo, useState } from "react";
import {
  Checkbox,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Spin,
  TimePicker,
} from "antd";
import { useDispatch, useSelector } from "react-redux";
import toast from "react-hot-toast";
import dayjs from "dayjs";
import {
  fetchNotificationFrequencies,
  save,
} from "../../features/save/saveSlice";
import svg45 from "../../assets/svg/svg45.svg";

const { TextArea } = Input;

const FREQUENCY_STEP = {
  DAILY: [1, "day"],
  WEEKLY: [1, "week"],
  MONTHLY: [1, "month"],
  EVERY_2_MONTHS: [2, "month"],
  EVERY_3_MONTHS: [3, "month"],
  EVERY_6_MONTHS: [6, "month"],
  YEARLY: [1, "year"],
};

// Mirrors the backend's advanceUntilFuture so the preview shows the real next run.
const computeNextRun = (startDate, sendTime, frequency, intervalDays) => {
  if (!startDate || !frequency) return null;
  const [h, m] = String(sendTime || "09:00").split(":");
  let next = dayjs(startDate).hour(Number(h) || 0).minute(Number(m) || 0).second(0);
  const step =
    frequency === "CUSTOM_DAYS"
      ? [Math.max(Number(intervalDays) || 1, 1), "day"]
      : FREQUENCY_STEP[frequency];
  if (!step) return null;
  const now = dayjs();
  for (let i = 0; i < 2000 && !next.isAfter(now); i++) next = next.add(step[0], step[1]);
  return next.isAfter(now) ? next : null;
};

export const FREQUENCY_FALLBACK = [
  { value: "DAILY", label: "Daily" },
  { value: "WEEKLY", label: "Weekly" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "EVERY_2_MONTHS", label: "Every 2 months" },
  { value: "EVERY_3_MONTHS", label: "Every 3 months" },
  { value: "EVERY_6_MONTHS", label: "Every 6 months" },
  { value: "YEARLY", label: "Yearly" },
  { value: "CUSTOM_DAYS", label: "Custom (every N days)" },
];

export const frequencyLabel = (value, options) => {
  if (!value) return "—";
  const match =
    options?.find((o) => o?.value === value) ||
    FREQUENCY_FALLBACK.find((o) => o?.value === value);
  return match?.label || value;
};

export const splitCsv = (value) =>
  String(value || "")
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean);

// Accepts 0712…, +254712…, 254712… or bare 712… and returns the 9 subscriber digits.
const normalizeMsisdn = (raw) => {
  let digits = String(raw || "").replace(/\D/g, "");
  if (digits.startsWith("254")) digits = digits.slice(3);
  else if (digits.startsWith("0")) digits = digits.slice(1);
  return digits.slice(0, 9);
};

const timeToDayjs = (hhmm) => {
  const [h, m] = String(hhmm || "09:00").split(":");
  return dayjs().hour(Number(h) || 0).minute(Number(m) || 0).second(0);
};

const ScheduledNotificationModal = ({
  isModalOpen,
  setIsModalOpen,
  prodd,
  handleFetch,
}) => {
  const [form] = Form.useForm();
  const dispatch = useDispatch();
  const { saving } = useSelector((state) => state.save);
  const [frequencies, setFrequencies] = useState(FREQUENCY_FALLBACK);

  const name = Form.useWatch("snName", form);
  const subject = Form.useWatch("snSubject", form);
  const message = Form.useWatch("snMessage", form);
  const frequency = Form.useWatch("snFrequency", form);
  const intervalDays = Form.useWatch("snIntervalDays", form);
  const sendTime = Form.useWatch("snSendTime", form);
  const startDate = Form.useWatch("snStartDate", form);
  const channels = Form.useWatch("snChannels", form);
  const phones = Form.useWatch("phones", form);
  const emails = Form.useWatch("emails", form);

  const smsOn = (channels || []).includes("SMS");
  const emailOn = (channels || []).includes("EMAIL");

  const phoneCount = (phones || []).filter((p) => p?.digits).length;
  const emailCount = (emails || []).filter((e) => e?.address).length;

  const charCount = message?.length || 0;
  const segments = Math.max(1, Math.ceil(charCount / 160));

  const handleCancel = () => setIsModalOpen(false);

  useEffect(() => {
    async function loadFrequencies() {
      const res = await dispatch(fetchNotificationFrequencies());
      const result = res?.payload?.data?.result;
      if (Array.isArray(result) && result.length > 0) setFrequencies(result);
    }
    loadFrequencies();
  }, []);

  useEffect(() => {
    if (!isModalOpen) return;
    form.resetFields();
    if (prodd) {
      const phoneRows = splitCsv(prodd?.snRecipients).map((v) => ({
        digits: normalizeMsisdn(v),
      }));
      const emailRows = splitCsv(prodd?.snEmails).map((v) => ({ address: v }));
      form.setFieldsValue({
        snName: prodd?.snName,
        snSubject: prodd?.snSubject,
        snMessage: prodd?.snMessage,
        snFrequency: prodd?.snFrequency,
        snIntervalDays: prodd?.snIntervalDays,
        snSendTime: timeToDayjs(prodd?.snSendTime),
        snStartDate: prodd?.snStartDate ? dayjs(prodd.snStartDate) : dayjs(),
        snChannels: splitCsv(prodd?.snChannels),
        phones: phoneRows.length ? phoneRows : [{ digits: "" }],
        emails: emailRows.length ? emailRows : [{ address: "" }],
      });
      return;
    }
    form.setFieldsValue({
      snSendTime: timeToDayjs("09:00"),
      snStartDate: dayjs(),
      snChannels: ["SMS"],
      phones: [{ digits: "" }],
      emails: [{ address: "" }],
    });
  }, [prodd, isModalOpen]);

  const scheduleSummary = useMemo(() => {
    const label =
      frequency === "CUSTOM_DAYS" && intervalDays
        ? `Every ${intervalDays} day${intervalDays > 1 ? "s" : ""}`
        : frequencyLabel(frequency, frequencies);
    const time = sendTime ? dayjs(sendTime).format("HH:mm") : "09:00";
    const start = startDate
      ? dayjs(startDate).format("DD MMM YYYY")
      : dayjs().format("DD MMM YYYY");
    const parts = [`${label} at ${time}, starting ${start}`];
    if (smsOn) parts.push(`${phoneCount} phone${phoneCount === 1 ? "" : "s"}`);
    if (emailOn) parts.push(`${emailCount} email${emailCount === 1 ? "" : "s"}`);
    return parts.join(" · ");
  }, [
    frequency,
    intervalDays,
    sendTime,
    startDate,
    smsOn,
    emailOn,
    phoneCount,
    emailCount,
    frequencies,
  ]);

  const nextRunLabel = useMemo(() => {
    const time = sendTime ? dayjs(sendTime).format("HH:mm") : "09:00";
    const next = computeNextRun(startDate, time, frequency, intervalDays);
    return next ? next.format("DD MMM YYYY, HH:mm") : null;
  }, [startDate, sendTime, frequency, intervalDays]);

  const onFinish = async (values) => {
    const recipients = (values?.phones || [])
      .map((p) => normalizeMsisdn(p?.digits))
      .filter((d) => d.length === 9)
      .map((d) => `254${d}`);
    const addresses = (values?.emails || [])
      .map((e) => String(e?.address || "").trim())
      .filter(Boolean);

    const payload = {
      snName: values?.snName,
      snSubject: values?.snSubject || null,
      snMessage: values?.snMessage,
      snFrequency: values?.snFrequency,
      snIntervalDays:
        values?.snFrequency === "CUSTOM_DAYS" ? values?.snIntervalDays : null,
      snSendTime: values?.snSendTime
        ? dayjs(values.snSendTime).format("HH:mm")
        : "09:00",
      snStartDate: values?.snStartDate
        ? dayjs(values.snStartDate).format("YYYY-MM-DD")
        : dayjs().format("YYYY-MM-DD"),
      snChannels: (values?.snChannels || []).join(","),
      snRecipients: smsOn ? recipients.join(",") : "",
      snEmails: emailOn ? addresses.join(",") : "",
    };

    const res = await dispatch(
      save({
        url: prodd?.snId
          ? `api/v2/notifications/update/${prodd?.snId}`
          : `api/v2/notifications/save`,
        ...payload,
      })
    );
    if (res?.payload?.success) {
      toast.success(res?.payload?.messages?.message);
      form.resetFields();
      setIsModalOpen(false);
      if (handleFetch) handleFetch();
    } else {
      toast.error(res?.payload?.messages?.message);
    }
  };

  return (
    <Modal
      title={prodd?.snId ? "Edit reminder" : "New reminder"}
      open={isModalOpen}
      onCancel={handleCancel}
      width={860}
      maskClosable={false}
      footer={null}
    >
      <Form
        layout="vertical"
        name="notification-form"
        onFinish={onFinish}
        form={form}
        style={{ maxWidth: "100%" }}
      >
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-x-6">
          <div className="lg:col-span-2">
            <span className="sa-eyebrow">Details</span>
            <Form.Item
              label="Reminder name"
              name="snName"
              rules={[{ required: true, message: "Required field" }]}
            >
              <Input
                className="input"
                placeholder="Usage Quota Expiry Reminder"
              />
            </Form.Item>

            <Form.Item label="Email subject" name="snSubject">
              <Input
                className="input"
                placeholder="Defaults to the reminder name"
              />
            </Form.Item>

            <Form.Item
              label="Message"
              name="snMessage"
              rules={[{ required: true, message: "Required field" }]}
              className="!mb-1"
            >
              <TextArea
                rows={4}
                className="input-textarea"
                placeholder="Your usage quota expires this week. Top up to stay connected."
              />
            </Form.Item>
            <p className="text-[12px] text-muted mb-5">
              {charCount} characters · {segments} SMS segment
              {segments > 1 ? "s" : ""} · sent as both the SMS and the email body
            </p>

            <span className="sa-eyebrow">Schedule</span>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-4">
              <Form.Item
                label="Frequency"
                name="snFrequency"
                rules={[{ required: true, message: "Required field" }]}
              >
                <Select
                  options={frequencies}
                  placeholder="How often should this fire?"
                  style={{ height: "42px" }}
                />
              </Form.Item>

              {frequency === "CUSTOM_DAYS" && (
                <Form.Item
                  label="Repeat every (days)"
                  name="snIntervalDays"
                  rules={[{ required: true, message: "Required field" }]}
                >
                  <InputNumber min={1} className="input" style={{ width: "100%" }} />
                </Form.Item>
              )}

              <Form.Item
                label="Start date"
                name="snStartDate"
                rules={[{ required: true, message: "Required field" }]}
                extra="Anchors the cycle. A past date is fine — the next run rolls forward from it."
              >
                <DatePicker
                  format="YYYY-MM-DD"
                  className="w-full"
                  style={{ height: "42px", width: "100%" }}
                />
              </Form.Item>

              <Form.Item
                label="Send time"
                name="snSendTime"
                rules={[{ required: true, message: "Required field" }]}
              >
                <TimePicker
                  format="HH:mm"
                  minuteStep={5}
                  className="w-full"
                  style={{ height: "42px", width: "100%" }}
                />
              </Form.Item>
            </div>

            <span className="sa-eyebrow">Channels</span>
            <Form.Item
              name="snChannels"
              rules={[{ required: true, message: "Pick at least one channel" }]}
            >
              <Checkbox.Group
                options={[
                  { label: "SMS", value: "SMS" },
                  { label: "Email", value: "EMAIL" },
                ]}
              />
            </Form.Item>

            {smsOn && (
              <>
                <span className="sa-eyebrow">Phone recipients</span>
                <Form.List
                  name="phones"
                  rules={[
                    {
                      validator: async (_, rows) => {
                        const entered = (rows || [])
                          .map((r) => r?.digits)
                          .filter(Boolean);
                        if (entered.length === 0)
                          return Promise.reject(
                            new Error("Add at least one phone number")
                          );
                        if (new Set(entered).size !== entered.length)
                          return Promise.reject(
                            new Error("Duplicate numbers are not allowed")
                          );
                      },
                    },
                  ]}
                >
                  {(fields, { add, remove }, { errors }) => (
                    <div className="mb-5">
                      {fields.map(({ key, name: rowName, ...restField }) => (
                        <div key={key} className="flex items-start gap-x-2 mb-1">
                          <Form.Item
                            {...restField}
                            name={[rowName, "digits"]}
                            normalize={normalizeMsisdn}
                            rules={[
                              {
                                required: true,
                                message: "Enter 9 digits starting with 7 or 1",
                              },
                              {
                                pattern: /^[71]\d{8}$/,
                                message: "Enter 9 digits starting with 7 or 1",
                              },
                            ]}
                            className="!mb-2 flex-1"
                          >
                            <Input
                              addonBefore="+254"
                              inputMode="numeric"
                              placeholder="712345678"
                              className="input"
                            />
                          </Form.Item>
                          <button
                            type="button"
                            disabled={fields.length === 1}
                            onClick={() => remove(rowName)}
                            className="bg-transparent mt-[6px] disabled:opacity-30"
                            aria-label="Remove number"
                          >
                            <img src={svg45} alt="remove" className="w-4 h-4" />
                          </button>
                        </div>
                      ))}
                      <div className="flex items-center justify-between">
                        <button
                          type="button"
                          onClick={() => add({ digits: "" })}
                          className="bg-transparent text-accent text-[13px] font-semibold"
                        >
                          ＋ Add another number
                        </button>
                        <span className="text-[12px] text-muted">
                          {phoneCount} recipient{phoneCount === 1 ? "" : "s"}
                        </span>
                      </div>
                      <Form.ErrorList errors={errors} />
                    </div>
                  )}
                </Form.List>
              </>
            )}

            {emailOn && (
              <>
                <span className="sa-eyebrow">Email recipients</span>
                <Form.List
                  name="emails"
                  rules={[
                    {
                      validator: async (_, rows) => {
                        const entered = (rows || [])
                          .map((r) => String(r?.address || "").toLowerCase())
                          .filter(Boolean);
                        if (entered.length === 0)
                          return Promise.reject(
                            new Error("Add at least one email address")
                          );
                        if (new Set(entered).size !== entered.length)
                          return Promise.reject(
                            new Error("Duplicate emails are not allowed")
                          );
                      },
                    },
                  ]}
                >
                  {(fields, { add, remove }, { errors }) => (
                    <div className="mb-5">
                      {fields.map(({ key, name: rowName, ...restField }) => (
                        <div key={key} className="flex items-start gap-x-2 mb-1">
                          <Form.Item
                            {...restField}
                            name={[rowName, "address"]}
                            rules={[
                              { required: true, message: "Required field" },
                              { type: "email", message: "Enter a valid email" },
                            ]}
                            className="!mb-2 flex-1"
                          >
                            <Input
                              className="input"
                              placeholder="finance@company.co.ke"
                            />
                          </Form.Item>
                          <button
                            type="button"
                            disabled={fields.length === 1}
                            onClick={() => remove(rowName)}
                            className="bg-transparent mt-[6px] disabled:opacity-30"
                            aria-label="Remove email"
                          >
                            <img src={svg45} alt="remove" className="w-4 h-4" />
                          </button>
                        </div>
                      ))}
                      <div className="flex items-center justify-between">
                        <button
                          type="button"
                          onClick={() => add({ address: "" })}
                          className="bg-transparent text-accent text-[13px] font-semibold"
                        >
                          ＋ Add another email
                        </button>
                        <span className="text-[12px] text-muted">
                          {emailCount} recipient{emailCount === 1 ? "" : "s"}
                        </span>
                      </div>
                      <p className="text-[12px] text-muted mt-1">
                        These addresses receive the branded Synq email.
                      </p>
                      <Form.ErrorList errors={errors} />
                    </div>
                  )}
                </Form.List>
              </>
            )}
          </div>

          <div className="lg:col-span-1 mt-4 lg:mt-0">
            <div className="card !p-4 bg-surface sticky top-0">
              <span className="sa-eyebrow">Preview</span>
              <p className="text-[13px] font-semibold text-primary break-words">
                {subject || name || "Reminder subject"}
              </p>
              <p className="text-[13px] text-ink mt-2 whitespace-pre-wrap break-words">
                {message || "Your message will appear here."}
              </p>
              <div className="border-t border-border mt-4 pt-3">
                <p className="text-[12px] text-muted">{scheduleSummary}</p>
                {nextRunLabel && (
                  <p className="text-[12px] text-primary font-semibold mt-2">
                    Next run: {nextRunLabel}
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="flex justify-end items-center gap-x-5 mt-8 mb-4">
          <div className="w-[150px]">
            <button
              type="button"
              onClick={handleCancel}
              className="cstm-btn !bg-white !text-[var(--brand)] !border !border-[var(--brand)]"
            >
              Cancel
            </button>
          </div>
          <div className="w-[200px]">
            <button type="submit" className="cstm-btn" disabled={saving}>
              {saving ? <Spin /> : prodd?.snId ? "Save changes" : "Create reminder"}
            </button>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default ScheduledNotificationModal;
