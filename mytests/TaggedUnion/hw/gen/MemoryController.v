// Generator : SpinalHDL dev    git head : ???
// Component : MemoryController

`timescale 1ns/1ps

module MemoryController (
  output reg           io_request_valid,
  input  wire          io_request_ready,
  output reg  [63:0]   io_request_payload_unionPayload,
  output reg  [0:0]    io_request_payload_tag,
  input  wire          io_response_valid,
  input  wire [31:0]   io_response_payload_unionPayload,
  input  wire [0:0]    io_response_payload_tag,
  input  wire          io_doReq,
  input  wire          io_rw,
  output reg  [31:0]   io_answer
);
  localparam SpinalEnum_read = 1'd0;
  localparam SpinalEnum_write = 1'd1;

  `ifndef SYNTHESIS
  reg [39:0] io_request_payload_tag_string;
  reg [39:0] io_response_payload_tag_string;
  `endif


  `ifndef SYNTHESIS
  always @(*) begin
    case(io_request_payload_tag)
      SpinalEnum_read : io_request_payload_tag_string = "read ";
      SpinalEnum_write : io_request_payload_tag_string = "write";
      default : io_request_payload_tag_string = "?????";
    endcase
  end
  always @(*) begin
    case(io_response_payload_tag)
      SpinalEnum_read : io_response_payload_tag_string = "read ";
      SpinalEnum_write : io_response_payload_tag_string = "write";
      default : io_response_payload_tag_string = "?????";
    endcase
  end
  `endif

  always @(*) begin
    io_request_payload_unionPayload = 64'bxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx;
    if(io_doReq) begin
      if(io_rw) begin
        io_request_payload_unionPayload[31 : 0] = 32'h00000002;
        io_request_payload_unionPayload[63 : 32] = 32'h00000000;
      end else begin
        io_request_payload_unionPayload[31 : 0] = 32'h00000001;
      end
    end
  end

  always @(*) begin
    io_request_payload_tag = (1'bx);
    if(io_doReq) begin
      if(io_rw) begin
        io_request_payload_tag = SpinalEnum_write;
      end else begin
        io_request_payload_tag = SpinalEnum_read;
      end
    end
  end

  always @(*) begin
    io_request_valid = 1'b0;
    if(io_doReq) begin
      io_request_valid = 1'b1;
    end
  end

  always @(*) begin
    io_answer = 32'bxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx;
    if(io_response_valid) begin
      case(io_response_payload_tag)
        SpinalEnum_write : begin
          if(io_response_payload_unionPayload[0]) begin
            io_answer = 32'h00000000;
          end else begin
            io_answer = 32'h00000001;
          end
        end
        default : begin
          io_answer = io_response_payload_unionPayload[31 : 0];
        end
      endcase
    end
  end


endmodule
