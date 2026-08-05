export class HttpError extends Error {
  readonly status: number;
  readonly payload: unknown;

  constructor(
    message: string,
    status: number,
    payload: unknown = null
  ) {
    super(message);

    this.name = "HttpError";
    this.status = status;
    this.payload = payload;
  }
}
