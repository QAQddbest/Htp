<p align="center">
  <a href="#">
    <img src="./doc/img/logo.png" alt="Htp Logo" width="100" height="100">
  </a>
</p>

<h2 align="center">Htp</h2>

---

<p align="center">
<font size="20px" color="red">H</font>ow to <font size="20px" color="red">T</font>ransfer file as fast as <font size="20px" color="red">P</font>ossible?<br>
👇<br>
<a href="https://github.com/QAQddbest/Htp">Use Htp!</a>
</p>

---

## Introduce

Htp is a simple online file system, a console application based on netty, clikt and kotest.

You can run Htp on every path in your file system, which will deploy that directory into web, offering  ability of browsing directory and downloading file.

## Advantage

1. Multiply thread downloading. Support 'Range' in http header.
2. Never OOM. Use chunked to transfer file, which makes it impossible to run into OOM.
3. Long connection supported. Support 'Connection' in http header.

## How to use

> **JRE is required!**

Browse into `bin` directory and you can find two executable file there.

`htp` is prepared for \*nix, and `htp.bat` is prepared for windows.

run: `htp -h` to display helping message.

```
> htp -h
Usage: htp [OPTIONS] PATH

Options:
  --version       Show the version and exit
  -p, --port INT  Port to deploy
  -h, --help      Show this message and exit

Arguments:
  PATH  Path to display
```

As you can see, `PATH` is required and `port` is alternative.

## TODO

Not yet, at last before I master how to upload file. XD