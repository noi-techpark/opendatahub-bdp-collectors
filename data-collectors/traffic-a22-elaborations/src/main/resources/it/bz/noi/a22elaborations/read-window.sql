-- SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
--
-- SPDX-License-Identifier: AGPL-3.0-or-later
SELECT * FROM a22.a22_traffic
      WHERE "timestamp" >= ? AND "timestamp" < ?
        and stationcode = ?